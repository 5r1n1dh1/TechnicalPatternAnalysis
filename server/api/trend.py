"""Trend endpoint."""
from fastapi import APIRouter, Query
from pydantic import BaseModel
from enum import Enum

router = APIRouter()


class Interval(str, Enum):
    interval_5m = "5m"
    interval_15m = "15m"
    interval_30m = "30m"
    interval_1h = "1h"
    interval_1d = "1d"
    interval_1w = "1w"
    interval_1M = "1M"


class Range(str, Enum):
    range_1D = "1D"
    range_5D = "5D"
    range_1M = "1M"
    range_3M = "3M"
    range_6M = "6M"
    range_1Y = "1Y"
    range_2Y = "2Y"
    range_5Y = "5Y"
    range_10Y = "10Y"


class TrendResponse(BaseModel):
    label: str
    strength: int
    ema20: float
    ema50: float
    adx: float


@router.get("/trend", response_model=TrendResponse)
def get_trend(
    symbol: str = Query(..., description="Stock symbol"),
    interval: Interval = Query(..., description="Time interval"),
    range: Range = Query(..., description="Data range"),
):
    """Get trend verdict."""
    return TrendResponse(label="Sideways", strength=50, ema20=0.0, ema50=0.0, adx=0.0)
