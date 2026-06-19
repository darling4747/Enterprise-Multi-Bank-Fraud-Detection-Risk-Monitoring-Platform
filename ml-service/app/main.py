from __future__ import annotations

import asyncio
import json
import logging
import time
from collections import deque
from contextlib import suppress
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from starlette.responses import StreamingResponse

from app.predictor import FraudPredictor
from app.schemas import PredictionRequest, PredictionResponse


MODEL_PATH = Path(__file__).resolve().parent / "model" / "fraud_model.pkl"
MODEL_RELOAD_INTERVAL_SECONDS = 30

app = FastAPI(title="Fraud Detection ML Service", version="1.0.0")
predictor = FraudPredictor(MODEL_PATH)
logger = logging.getLogger("uvicorn.error")
runtime_logs: deque[dict[str, Any]] = deque(maxlen=200)
model_reload_task: asyncio.Task[None] | None = None
current_model_mtime: float | None = None


def _model_mtime() -> float | None:
    if not MODEL_PATH.exists() or MODEL_PATH.stat().st_size < 100:
        return None
    return MODEL_PATH.stat().st_mtime


def _format_timestamp(timestamp: float | None) -> str | None:
    if timestamp is None:
        return None
    return datetime.fromtimestamp(timestamp, timezone.utc).isoformat()


def add_runtime_log(event_type: str, status: str = "SUCCESS", **details: Any) -> None:
    entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "eventType": event_type,
        "status": status,
        **details,
    }
    runtime_logs.appendleft(entry)
    print(_terminal_log_line(entry), flush=True)


def _terminal_log_line(entry: dict[str, Any]) -> str:
    detail_text = " ".join(
        f"{key}={_terminal_value(value)}"
        for key, value in entry.items()
        if key not in {"timestamp", "eventType", "status"} and value is not None
    )
    base = f"[{entry['timestamp']}] {entry['eventType']} status={entry['status']}"
    return f"{base} {detail_text}" if detail_text else base


def _terminal_value(value: Any) -> str:
    if isinstance(value, float):
        return f"{value:.6f}"
    return str(value).replace("\n", " ")


@app.middleware("http")
async def request_logger(request: Request, call_next):
    start = time.perf_counter()
    try:
        response = await call_next(request)
        duration_ms = round((time.perf_counter() - start) * 1000, 2)
        if request.url.path not in {"/logs/stream"}:
            add_runtime_log(
                "HTTP_REQUEST",
                "SUCCESS" if response.status_code < 400 else "FAILURE",
                method=request.method,
                path=request.url.path,
                statusCode=response.status_code,
                durationMs=duration_ms,
                client=request.client.host if request.client else None,
            )
        logger.info(
            "HTTP_REQUEST method=%s path=%s status=%s duration_ms=%.2f",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
        )
        return response
    except Exception as exc:
        duration_ms = round((time.perf_counter() - start) * 1000, 2)
        add_runtime_log(
            "HTTP_REQUEST",
            "FAILURE",
            method=request.method,
            path=request.url.path,
            durationMs=duration_ms,
            message=str(exc),
        )
        raise


async def watch_model_file() -> None:
    global current_model_mtime

    while True:
        await asyncio.sleep(MODEL_RELOAD_INTERVAL_SECONDS)
        try:
            latest_mtime = _model_mtime()
            if latest_mtime is None:
                add_runtime_log(
                    "MODEL_CHECK",
                    "WARNING",
                    message="Trained model file is missing or empty.",
                    modelPath=str(MODEL_PATH),
                )
                logger.warning("MODEL_CHECK trained model file is missing or empty: %s", MODEL_PATH)
                continue

            if current_model_mtime != latest_mtime:
                predictor.load_model()
                current_model_mtime = latest_mtime
                add_runtime_log(
                    "MODEL_RELOADED",
                    modelLoaded=predictor.is_loaded,
                    modelVersion=predictor.model_version,
                    modelUpdatedAt=_format_timestamp(current_model_mtime),
                )
                logger.info("MODEL_RELOADED model_version=%s", predictor.model_version)
                continue

            add_runtime_log(
                "MODEL_CHECK",
                modelLoaded=predictor.is_loaded,
                modelVersion=predictor.model_version,
                modelUpdatedAt=_format_timestamp(current_model_mtime),
            )
            logger.info(
                "MODEL_CHECK model_loaded=%s model_version=%s",
                predictor.is_loaded,
                predictor.model_version,
            )
        except Exception as exc:
            add_runtime_log("MODEL_RELOAD_FAILED", "FAILURE", message=str(exc))
            logger.exception("MODEL_RELOAD_FAILED")


@app.on_event("startup")
async def startup() -> None:
    global current_model_mtime, model_reload_task

    current_model_mtime = _model_mtime()
    add_runtime_log(
        "SERVICE_STARTED",
        modelLoaded=predictor.is_loaded,
        modelPath=str(MODEL_PATH),
        modelVersion=predictor.model_version,
        modelUpdatedAt=_format_timestamp(current_model_mtime),
        reloadIntervalSeconds=MODEL_RELOAD_INTERVAL_SECONDS,
    )
    logger.info(
        "SERVICE_STARTED model_loaded=%s model_version=%s reload_interval_seconds=%s",
        predictor.is_loaded,
        predictor.model_version,
        MODEL_RELOAD_INTERVAL_SECONDS,
    )
    model_reload_task = asyncio.create_task(watch_model_file())


@app.on_event("shutdown")
async def shutdown() -> None:
    if model_reload_task:
        model_reload_task.cancel()
        with suppress(asyncio.CancelledError):
            await model_reload_task


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "Fraud Detection ML Service"}


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "status": "ok" if predictor.is_loaded else "model_not_loaded",
        "model_loaded": predictor.is_loaded,
        "model_path": str(MODEL_PATH),
        "model_version": predictor.model_version,
        "model_updated_at": _format_timestamp(current_model_mtime or _model_mtime()),
        "reload_interval_seconds": MODEL_RELOAD_INTERVAL_SECONDS,
    }


@app.get("/logs")
def logs(limit: int = 100) -> dict[str, object]:
    bounded_limit = max(1, min(limit, 200))
    return {
        "count": len(runtime_logs),
        "logs": list(runtime_logs)[:bounded_limit],
    }


@app.get("/logs/stream")
async def stream_logs() -> StreamingResponse:
    async def event_stream():
        last_seen: str | None = None
        while True:
            newest = runtime_logs[0] if runtime_logs else None
            if newest and newest.get("timestamp") != last_seen:
                last_seen = str(newest.get("timestamp"))
                yield "data: " + json.dumps(newest, default=str) + "\n\n"
            await asyncio.sleep(1)

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@app.get("/model/feature-importance")
def feature_importance() -> dict[str, object]:
    importance = predictor.feature_importance()
    add_runtime_log(
        "FEATURE_IMPORTANCE_REQUESTED",
        modelVersion=importance.get("model_version"),
        featureCount=len(importance.get("feature_importance", [])),
    )
    return importance


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest) -> PredictionResponse:
    try:
        payload = request.model_dump()
    except AttributeError:
        payload = request.dict()

    try:
        add_runtime_log(
            "TRANSACTION_RECEIVED",
            transactionId=payload.get("transactionId"),
            reference=payload.get("reference"),
            transactionType=payload.get("type"),
            amount=payload.get("amount"),
        )
        logger.info(
            "TRANSACTION_RECEIVED transaction_id=%s reference=%s type=%s amount=%s",
            payload.get("transactionId"),
            payload.get("reference"),
            payload.get("type"),
            payload.get("amount"),
        )
        add_runtime_log(
            "RUNNING_PREDICTION",
            transactionId=payload.get("transactionId"),
            reference=payload.get("reference"),
            transactionType=payload.get("type"),
            amount=payload.get("amount"),
            modelVersion=predictor.model_version,
        )
        logger.info("RUNNING_PREDICTION reference=%s", payload.get("reference"))
        response = predictor.predict(payload)
        add_runtime_log(
            "PREDICTION",
            transactionId=payload.get("transactionId"),
            reference=payload.get("reference"),
            fraudProbability=response.fraudProbability,
            mlRiskScore=response.mlRiskScore,
            modelVersion=predictor.model_version,
        )
        logger.info(
            "PREDICTION_COMPLETED reference=%s fraud_probability=%.6f ml_risk_score=%s",
            payload.get("reference"),
            response.fraudProbability,
            response.mlRiskScore,
        )
        logger.info(
            "PREDICTION_SENT_TO_BACKEND transaction_id=%s reference=%s ml_risk_score=%s",
            payload.get("transactionId"),
            payload.get("reference"),
            response.mlRiskScore,
        )
        add_runtime_log(
            "PREDICTION_SENT_TO_BACKEND",
            transactionId=payload.get("transactionId"),
            reference=payload.get("reference"),
            fraudProbability=response.fraudProbability,
            mlRiskScore=response.mlRiskScore,
        )
        return response
    except RuntimeError as exc:
        add_runtime_log("PREDICTION_FAILED", "FAILURE", message=str(exc))
        raise HTTPException(status_code=503, detail=str(exc)) from exc
