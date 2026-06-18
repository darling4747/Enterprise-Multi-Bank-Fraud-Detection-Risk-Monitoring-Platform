from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    precision_recall_curve,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

try:
    from xgboost import XGBClassifier
except ImportError:
    XGBClassifier = None


SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.features import (  # noqa: E402
    CATEGORICAL_FEATURES,
    FEATURE_COLUMNS,
    NUMERIC_FEATURES,
    TARGET_COLUMN,
    clean_transactions,
    prepare_feature_frame,
)


DATASET_PATH = SERVICE_ROOT / "training" / "Synthetic Financial Datasets.csv"
CLEANED_SAMPLE_PATH = SERVICE_ROOT / "training" / "cleaned_transactions_sample.csv"
MODEL_PATH = SERVICE_ROOT / "app" / "model" / "fraud_model.pkl"
METADATA_PATH = SERVICE_ROOT / "app" / "model" / "fraud_model_metrics.json"

RANDOM_STATE = 42
MAX_LEGITIMATE_ROWS = 250_000
MIN_RECALL_FOR_THRESHOLD = 0.88


def train() -> None:
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset not found: {DATASET_PATH}")

    print(f"Loading raw dataset: {DATASET_PATH}")
    raw_df = pd.read_csv(DATASET_PATH)
    cleaned_df, cleaning_report = clean_transactions(raw_df)

    if cleaning_report["fraud_rows"] == 0:
        raise ValueError("The dataset has no fraud rows after cleaning.")

    training_df = stratified_training_sample(cleaned_df)
    training_df.to_csv(CLEANED_SAMPLE_PATH, index=False)
    print(f"Saved cleaned training sample: {CLEANED_SAMPLE_PATH}")

    x = prepare_feature_frame(training_df)
    y = training_df[TARGET_COLUMN]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        stratify=y,
        random_state=RANDOM_STATE,
    )

    positive_weight = max(1.0, (y_train == 0).sum() / max((y_train == 1).sum(), 1))
    model, estimator_name = build_pipeline(positive_weight)
    print(f"Training model with {len(x_train):,} rows and {len(FEATURE_COLUMNS)} features...")
    model.fit(x_train, y_train)

    probabilities = model.predict_proba(x_test)[:, 1]
    threshold = choose_threshold(y_test, probabilities)
    predictions = (probabilities >= threshold).astype(int)

    metrics = build_metrics(
        cleaning_report=cleaning_report,
        training_rows=len(training_df),
        test_rows=len(x_test),
        y_test=y_test,
        probabilities=probabilities,
        predictions=predictions,
        threshold=threshold,
        estimator_name=estimator_name,
        model=model,
    )

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    package = {
        "model": model,
        "threshold": threshold,
        "features": FEATURE_COLUMNS,
        "numeric_features": NUMERIC_FEATURES,
        "categorical_features": CATEGORICAL_FEATURES,
        "metadata": metrics,
    }
    joblib.dump(package, MODEL_PATH)
    METADATA_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    print(f"Saved model: {MODEL_PATH}")
    print(f"Saved metrics: {METADATA_PATH}")
    print(json.dumps(metrics["evaluation"], indent=2))


def stratified_training_sample(cleaned_df: pd.DataFrame) -> pd.DataFrame:
    fraud = cleaned_df[cleaned_df[TARGET_COLUMN] == 1]
    legitimate = cleaned_df[cleaned_df[TARGET_COLUMN] == 0]

    if len(legitimate) > MAX_LEGITIMATE_ROWS:
        legitimate = legitimate.sample(n=MAX_LEGITIMATE_ROWS, random_state=RANDOM_STATE)

    sampled = pd.concat([fraud, legitimate], axis=0)
    sampled = sampled.sort_values(["step", "type", "amount"], kind="mergesort").reset_index(drop=True)
    return sampled


def build_pipeline(positive_weight: float) -> tuple[Pipeline, str]:
    numeric_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )
    categorical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("onehot", OneHotEncoder(handle_unknown="ignore")),
        ]
    )

    preprocessor = ColumnTransformer(
        transformers=[
            ("numeric", numeric_transformer, NUMERIC_FEATURES),
            ("categorical", categorical_transformer, CATEGORICAL_FEATURES),
        ]
    )

    if XGBClassifier is not None:
        classifier = XGBClassifier(
            objective="binary:logistic",
            eval_metric="logloss",
            tree_method="hist",
            n_estimators=300,
            max_depth=5,
            learning_rate=0.05,
            subsample=0.9,
            colsample_bytree=0.9,
            scale_pos_weight=positive_weight,
            n_jobs=-1,
            random_state=RANDOM_STATE,
        )
        estimator_name = "xgboost.XGBClassifier"
    else:
        classifier = LogisticRegression(
            class_weight="balanced",
            max_iter=1000,
            random_state=RANDOM_STATE,
        )
        estimator_name = "sklearn.linear_model.LogisticRegression"

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    ), estimator_name


def choose_threshold(y_true: pd.Series, probabilities) -> float:
    precision, recall, thresholds = precision_recall_curve(y_true, probabilities)
    if len(thresholds) == 0:
        return 0.5

    scored = []
    for idx, threshold in enumerate(thresholds):
        if recall[idx] < MIN_RECALL_FOR_THRESHOLD:
            continue
        f1 = 0.0
        if precision[idx] + recall[idx] > 0:
            f1 = 2 * precision[idx] * recall[idx] / (precision[idx] + recall[idx])
        scored.append((f1, threshold))

    if not scored:
        return 0.5

    scored.sort(key=lambda item: item[0], reverse=True)
    return float(scored[0][1])


def build_metrics(
    cleaning_report: dict[str, int],
    training_rows: int,
    test_rows: int,
    y_test: pd.Series,
    probabilities,
    predictions,
    threshold: float,
    estimator_name: str,
    model: Pipeline,
) -> dict:
    matrix = confusion_matrix(y_test, predictions, labels=[0, 1])
    report = classification_report(y_test, predictions, labels=[0, 1], output_dict=True, zero_division=0)

    metrics = {
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "source_dataset": str(DATASET_PATH),
        "cleaned_sample": str(CLEANED_SAMPLE_PATH),
        "features": FEATURE_COLUMNS,
        "cleaning": cleaning_report,
        "training": {
            "rows": int(training_rows),
            "test_rows": int(test_rows),
            "max_legitimate_rows": MAX_LEGITIMATE_ROWS,
            "random_state": RANDOM_STATE,
            "estimator": estimator_name,
        },
        "evaluation": {
            "threshold": threshold,
            "roc_auc": float(roc_auc_score(y_test, probabilities)),
            "average_precision": float(average_precision_score(y_test, probabilities)),
            "confusion_matrix": {
                "true_negative": int(matrix[0][0]),
                "false_positive": int(matrix[0][1]),
                "false_negative": int(matrix[1][0]),
                "true_positive": int(matrix[1][1]),
            },
            "classification_report": report,
        },
    }
    metrics["feature_importance"] = feature_importance(model)
    return metrics


def feature_importance(model: Pipeline) -> list[dict]:
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
    except Exception as exc:
        return [{"feature": "feature_importance_unavailable", "importance": 0.0, "error": str(exc)}]


if __name__ == "__main__":
    train()
