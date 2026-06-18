from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd


RAW_NUMERIC_COLUMNS = [
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

RAW_CATEGORICAL_COLUMNS = ["type"]

NUMERIC_FEATURES = [
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

CATEGORICAL_FEATURES = ["type"]

FEATURE_COLUMNS = NUMERIC_FEATURES + CATEGORICAL_FEATURES

TARGET_COLUMN = "isFraud"

KNOWN_TYPES = {"PAYMENT", "TRANSFER", "CASH_OUT", "DEBIT", "CASH_IN"}


def normalize_type(value: Any) -> str:
    if value is None or pd.isna(value):
        return "UNKNOWN"
    normalized = str(value).strip().upper().replace(" ", "_").replace("-", "_")
    return normalized if normalized in KNOWN_TYPES else "UNKNOWN"


def infer_type(channel: Any = None, merchant_category: Any = None, explicit_type: Any = None) -> str:
    if explicit_type:
        return normalize_type(explicit_type)

    candidates = [merchant_category, channel]
    for candidate in candidates:
        if candidate is None:
            continue
        value = str(candidate).strip().upper().replace(" ", "_").replace("-", "_")
        if value in KNOWN_TYPES:
            return value
        if value in {"WIRE_TRANSFER", "BANK_TRANSFER", "TRANSFER"}:
            return "TRANSFER"
        if value in {"ATM", "ATM_WITHDRAWAL", "WITHDRAWAL"}:
            return "CASH_OUT"
        if value in {"DEPOSIT", "CASH_DEPOSIT"}:
            return "CASH_IN"
        if value in {"CARD", "POS", "ECOMMERCE", "ONLINE", "MOBILE", "RETAIL"}:
            return "PAYMENT"

    return "PAYMENT"


def clean_transactions(raw: pd.DataFrame) -> tuple[pd.DataFrame, dict[str, int]]:
    df = raw.copy()
    df.columns = [column.strip() for column in df.columns]

    for column in RAW_NUMERIC_COLUMNS + RAW_CATEGORICAL_COLUMNS + [TARGET_COLUMN]:
        if column not in df.columns:
            df[column] = np.nan

    report: dict[str, int] = {"raw_rows": int(len(df))}

    for column in RAW_NUMERIC_COLUMNS + [TARGET_COLUMN]:
        df[column] = pd.to_numeric(df[column], errors="coerce")

    for column in RAW_CATEGORICAL_COLUMNS:
        df[column] = df[column].astype("string").str.strip()

    before = len(df)
    df = df.dropna(subset=["amount", TARGET_COLUMN])
    report["dropped_missing_required"] = int(before - len(df))

    before = len(df)
    finite_mask = np.isfinite(df[["amount", TARGET_COLUMN]].to_numpy(dtype=float)).all(axis=1)
    df = df.loc[finite_mask]
    report["dropped_non_finite_required"] = int(before - len(df))

    before = len(df)
    valid_mask = (
        (df["amount"] >= 0)
        & (df[TARGET_COLUMN].isin([0, 1]))
    )
    df = df.loc[valid_mask]
    report["dropped_invalid_required"] = int(before - len(df))

    for column in ["oldbalanceOrg", "newbalanceOrig", "oldbalanceDest", "newbalanceDest"]:
        df[column] = df[column].replace([np.inf, -np.inf], np.nan).fillna(0).clip(lower=0)

    df[TARGET_COLUMN] = df[TARGET_COLUMN].astype("int8")
    df["type"] = df["type"].map(normalize_type)

    before = len(df)
    df = df.drop_duplicates()
    report["dropped_duplicates"] = int(before - len(df))

    sort_columns = [column for column in ["step", "type", "amount"] if column in df.columns]
    df = df.sort_values(sort_columns, kind="mergesort").reset_index(drop=True)
    report["clean_rows"] = int(len(df))
    report["fraud_rows"] = int(df[TARGET_COLUMN].sum())
    report["legitimate_rows"] = int(len(df) - df[TARGET_COLUMN].sum())

    return df, report


def prepare_feature_frame(raw: pd.DataFrame) -> pd.DataFrame:
    df = raw.copy()
    for column in RAW_NUMERIC_COLUMNS:
        if column not in df.columns:
            df[column] = 0
        df[column] = pd.to_numeric(df[column], errors="coerce").replace([np.inf, -np.inf], np.nan)

    if "type" not in df.columns:
        df["type"] = "PAYMENT"

    df["amount"] = df["amount"].fillna(0).clip(lower=0)

    for column in ["oldbalanceOrg", "newbalanceOrig", "oldbalanceDest", "newbalanceDest"]:
        df[column] = df[column].fillna(0).clip(lower=0)

    df["type"] = df["type"].map(normalize_type)

    return df[FEATURE_COLUMNS]


def request_to_model_row(payload: dict[str, Any]) -> dict[str, Any]:
    amount = _as_float(payload.get("amount"), default=0.0)
    transaction_type = infer_type(
        channel=payload.get("channel"),
        merchant_category=payload.get("merchantCategory"),
        explicit_type=payload.get("type"),
    )

    old_origin = _optional_float(payload.get("oldbalanceOrg"))
    new_origin = _optional_float(payload.get("newbalanceOrig"))
    old_destination = _optional_float(payload.get("oldbalanceDest"))
    new_destination = _optional_float(payload.get("newbalanceDest"))

    if old_origin is None:
        old_origin = amount
    if new_origin is None:
        new_origin = max(old_origin - amount, 0.0)
    if old_destination is None:
        old_destination = 0.0
    if new_destination is None:
        new_destination = old_destination + amount

    return {
        "type": transaction_type,
        "amount": amount,
        "oldbalanceOrg": old_origin,
        "newbalanceOrig": new_origin,
        "oldbalanceDest": old_destination,
        "newbalanceDest": new_destination,
    }


def _optional_float(value: Any) -> float | None:
    if value is None or value == "":
        return None
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    if not np.isfinite(number):
        return None
    return max(number, 0.0)


def _as_float(value: Any, default: float) -> float:
    number = _optional_float(value)
    return default if number is None else number
