from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class PredictionRequest(BaseModel):
    transactionId: Optional[int] = None
    reference: Optional[str] = None
    amount: float = Field(..., ge=0)
    type: Optional[str] = None
    oldbalanceOrg: Optional[float] = Field(default=None, ge=0)
    newbalanceOrig: Optional[float] = Field(default=None, ge=0)
    oldbalanceDest: Optional[float] = Field(default=None, ge=0)
    newbalanceDest: Optional[float] = Field(default=None, ge=0)


class PredictionResponse(BaseModel):
    fraudProbability: float
    mlRiskScore: int
