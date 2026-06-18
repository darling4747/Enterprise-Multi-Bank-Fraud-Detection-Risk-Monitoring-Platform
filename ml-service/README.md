# ML Service

The Machine Learning Service is a standalone FastAPI microservice responsible for analyzing banking transactions and estimating the probability of fraudulent activity. It is the intelligence layer of the Fraud Detection and Risk Monitoring Platform: it validates transaction data, prepares model features, evaluates fraud patterns, and returns real-time risk predictions to the Spring Boot backend.

The service is deployed independently from the main banking backend. The model can be cleaned, trained, updated, and restarted without changing the core Spring Boot application.

## Objective

Identify suspicious transactions before they are approved or completed. The service analyzes transaction behavior, balances, transaction types, and engineered risk patterns, then returns a fraud probability, risk score, risk level, and recommended decision.

## Responsibilities

- Fraud prediction for incoming transactions.
- Risk scoring from 0 to 100.
- Risk classification into `LOW`, `MEDIUM`, or `HIGH`.
- Real-time transaction analysis for backend workflows.
- Pattern recognition across transaction type, amount, account balance deltas, and suspicious balance errors.
- Decision support for fraud analysts and automated prevention rules.

## Technology Stack

- Python
- FastAPI
- Uvicorn
- XGBoost
- Scikit-learn
- Pandas
- NumPy
- Joblib
- SHAP

## Train

```powershell
python training\train_model.py
```

Outputs:

- `app/model/fraud_model.pkl`
- `app/model/fraud_model_metrics.json`
- `training/cleaned_transactions_sample.csv`

## Run

```powershell
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8000/health
```

## Workflow

1. Transaction data is received from the Spring Boot backend.
2. The request is validated by FastAPI/Pydantic.
3. Missing PaySim balance fields are normalized into neutral runtime defaults.
4. Model features are prepared using the same transformations used during training.
5. The trained fraud detection model generates a fraud probability.
6. The probability is converted into a risk score from 0 to 100.
7. The service returns:
   - `APPROVE`
   - `REVIEW`
   - `BLOCK`

## Input Features

Core training features:

- Transaction type
- Transaction amount
- Sender balance before transaction
- Sender balance after transaction
- Receiver balance before transaction
- Receiver balance after transaction

Runtime enterprise fields accepted by the API:

- Device ID
- IP address
- Geo/country
- Customer ID
- Source account
- Destination account
- Merchant category
- Channel

## Prediction Output

```json
{
  "riskScore": 91,
  "riskLevel": "HIGH",
  "decision": "BLOCK",
  "fraudProbability": 0.94
}
```

The API also returns snake_case aliases used by backend integrations, including `risk_score`, `risk_level`, `probability`, and `fraud_probability`.

## Risk Classification

- `LOW`: risk score `0-30`, transaction approved.
- `MEDIUM`: risk score `31-70`, transaction flagged for review.
- `HIGH`: risk score `71-100`, transaction blocked or escalated.

## System Integration

```text
Frontend React Dashboard
  -> Spring Boot Backend
  -> ML Service FastAPI
  -> Fraud Detection Model XGBoost
```

The frontend never calls the ML service directly. All communication flows through the Spring Boot backend for authentication, validation, database persistence, and centralized business rules.

## Future Enhancements

- Real-time streaming analysis using Kafka.
- Continuous model retraining.
- Deep learning fraud models.
- SHAP-powered explainability dashboards.
- Behavioral fraud detection.
- Customer risk profiling.
- Multi-model ensemble fraud prediction.
