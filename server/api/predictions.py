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

# Module-level cache refreshed only on deploy/reload
_cache = None

def _load():
    global _cache
    try:
        raw = load_predictions()
        _cache = raw
    except StalePredictionsError as e:
        _cache = {"stale": True, "message": str(e)}
    return _cache

@router.get("/predictions")
def get_predictions(symbol: Optional[str] = None):
    data = _load()
    if isinstance(data, dict) and data.get("stale"):
        from fastapi import HTTPException
        raise HTTPException(status_code=503, detail=data["message"])
    skipped = data.get("skipped", [])
    preds = data.get("predictions", [])
    if symbol:
        preds = [p for p in preds if p.get("symbol") == symbol]
    return {
        "asOf": data.get("asOf"),
        "predictions": preds,
        "skipped": skipped
    }
