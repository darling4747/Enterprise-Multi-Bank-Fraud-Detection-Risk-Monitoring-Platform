from __future__ import annotations

import json
import sys
from pathlib import Path

SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.predictor import FraudPredictor  # noqa: E402


MODEL_PATH = SERVICE_ROOT / "app" / "model" / "fraud_model.pkl"
REPORT_PATH = SERVICE_ROOT / "app" / "model" / "model_behavior_report.json"


SAMPLES = [
    {
        "name": "individual_normal_transfer_1000",
        "type": "TRANSFER",
        "amount": 1_000,
        "oldbalanceOrg": 50_000,
        "newbalanceOrig": 49_000,
        "oldbalanceDest": 10_000,
        "newbalanceDest": 11_000,
    },
    {
        "name": "individual_large_transfer_10000000_normal_balances",
        "type": "TRANSFER",
        "amount": 10_000_000,
        "oldbalanceOrg": 50_000_000,
        "newbalanceOrig": 40_000_000,
        "oldbalanceDest": 1_000_000,
        "newbalanceDest": 11_000_000,
    },
    {
        "name": "same_large_transfer_with_context_ignored_by_ml",
        "type": "TRANSFER",
        "amount": 10_000_000,
        "oldbalanceOrg": 50_000_000,
        "newbalanceOrig": 40_000_000,
        "oldbalanceDest": 1_000_000,
        "newbalanceDest": 11_000_000,
        "accountType": "CORPORATE",
        "beneficiaryTrusted": True,
        "knownDevice": True,
        "knownLocation": True,
        "transactionHour": 11,
    },
    {
        "name": "large_transfer_balance_mismatch",
        "type": "TRANSFER",
        "amount": 10_000_000,
        "oldbalanceOrg": 10_000_000,
        "newbalanceOrig": 10_000_000,
        "oldbalanceDest": 1_000_000,
        "newbalanceDest": 1_000_000,
    },
]


def main() -> None:
    predictor = FraudPredictor(MODEL_PATH)
    if not predictor.is_loaded:
        raise RuntimeError(f"Model is not trained or cannot be loaded: {MODEL_PATH}")

    predictions = []
    for sample in SAMPLES:
        response = predictor.predict(sample)
        predictions.append(
            {
                "name": sample["name"],
                "type": sample["type"],
                "amount": sample["amount"],
                "fraudProbability": response.fraudProbability,
                "mlRiskScore": response.mlRiskScore,
            }
        )

    report = {
        "modelVersion": predictor.model_version,
        "featureImportance": predictor.feature_importance()["feature_importance"],
        "predictions": predictions,
        "interpretation": "ML returns probability only. Spring Boot applies account, beneficiary, device, location, and hour rules to calculate the final risk score.",
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2), flush=True)


if __name__ == "__main__":
    main()
    raise SystemExit(0)
