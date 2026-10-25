"""GET /api/predictions endpoint (cached import, 503 if stale)."""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional
import json, os
from core.ml_predict import load_predictions, StalePredictionsError

router = APIRouter()

class HorizonPred(BaseModel):
    up: float
    down: float
    sideways: float

class Metrics(BaseModel):
    accuracy: float
    logLoss: float
    brier: float

class PredictionItem(BaseModel):
    symbol: str
    close: float
    h1: HorizonPred
    h2: HorizonPred
    h5: HorizonPred
    metrics: dict

class PredictionsResponse(BaseModel):
    asOf: str
    predictions: List[PredictionItem]
    skipped: List[str]

def _load():
    # Always read from disk; predictions.json ≤ 30 KB — freshness beats cache.
    return load_predictions()

@router.get("/predictions")
def get_predictions(symbol: Optional[str] = None):
    from core.errors import ApiError
    try:
        data = _load()
    except StalePredictionsError as e:
        raise ApiError(503, "upstreamUnavailable", detail=str(e))
    skipped = data.get("skipped", [])
    preds = data.get("predictions", [])
    if symbol:
        preds = [p for p in preds if p.get("symbol") == symbol]
    return PredictionsResponse(asOf=data.get("asOf"), predictions=preds, skipped=skipped)
