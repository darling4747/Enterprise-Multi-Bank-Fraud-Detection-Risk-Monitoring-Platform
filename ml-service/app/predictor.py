from __future__ import annotations

from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.features import prepare_feature_frame, request_to_model_row
from app.schemas import PredictionResponse


class FraudPredictor:
    def __init__(self, model_path: str | Path):
        self.model_path = Path(model_path)
        self.model = None
        self.threshold = 0.5
        self.metadata: dict[str, Any] = {}
        self.load_model()

    @property
    def is_loaded(self) -> bool:
        return self.model is not None

    @property
    def model_version(self) -> str:
        return str(self.metadata.get("trained_at", "untrained"))

    def load_model(self) -> None:
        if not self.model_path.exists() or self.model_path.stat().st_size < 100:
            return

        package = joblib.load(self.model_path)
        if isinstance(package, dict) and "model" in package:
            self.model = package["model"]
            self.threshold = float(package.get("threshold", 0.5))
            self.metadata = dict(package.get("metadata", {}))
            return

        self.model = package
        self.metadata = {"trained_at": "legacy-model"}

    def predict(self, payload: dict[str, Any]) -> PredictionResponse:
        if not self.is_loaded:
            raise RuntimeError("Fraud model is not trained or could not be loaded.")

        row = request_to_model_row(payload)
        features = prepare_feature_frame(pd.DataFrame([row]))
        probability = float(self.model.predict_proba(features)[0][1])
        probability = max(0.0, min(1.0, probability))
        return PredictionResponse(
            fraudProbability=probability,
            mlRiskScore=round(probability * 100),
        )

    def feature_importance(self) -> dict[str, Any]:
        metadata_importance = self.metadata.get("feature_importance")
        if metadata_importance:
            return {
                "model_version": self.model_version,
                "feature_importance": metadata_importance,
                "source": "training_metadata",
            }

        if not self.is_loaded:
            return {
                "model_version": self.model_version,
                "feature_importance": [],
                "source": "model_not_loaded",
            }

        return {
            "model_version": self.model_version,
            "feature_importance": compute_feature_importance(self.model),
            "source": "runtime_model",
        }


def compute_feature_importance(model: Any) -> list[dict[str, Any]]:
    try:
        preprocessor = model.named_steps["preprocessor"]
        classifier = model.named_steps["classifier"]
        feature_names = preprocessor.get_feature_names_out()
        importances = getattr(classifier, "feature_importances_", None)
        if importances is None:
            coefficients = getattr(classifier, "coef_", None)
            if coefficients is None:
                return []
            importances = abs(coefficients[0])
        rows = [
            {
                "feature": str(feature).replace("numeric__", "").replace("categorical__", ""),
                "importance": float(importance),
            }
            for feature, importance in zip(feature_names, importances, strict=False)
        ]
        rows.sort(key=lambda row: row["importance"], reverse=True)
        total = sum(row["importance"] for row in rows) or 1.0
        for row in rows:
            row["normalizedImportance"] = row["importance"] / total
        return rows[:25]
    except Exception:
        return []
