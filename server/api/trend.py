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
    from core.bars import load_bars
    from core.indicators import ema_set, adx_di
    from core.trend import trend_from_series, trend_strength
    import numpy as np
    df = load_bars(symbol, interval, range)
    if df is None or len(df) < 30:
        from fastapi import HTTPException
        raise HTTPException(status_code=503, detail="upstreamUnavailable")
    close = df["c"].to_numpy(dtype=float)
    high = df["h"].to_numpy(dtype=float)
    low = df["l"].to_numpy(dtype=float)
    ema20_arr, _, _ = ema_set(close)
    _, plus_di, minus_di = adx_di(high, low, close)
    tr = trend_from_series(close, ema20_arr, ema20_arr, float(plus_di), float(minus_di), 28.0)
    label = tr.get("label") or "Sideways"
    strength = trend_strength(tr.get("score", 50.0))
    return TrendResponse(
        symbol=symbol, interval=interval, range=range,
        label=label, strength=strength, ema20=float(ema20_arr[-1]), ema50=float(ema20_arr[-1]), adx=float(tr.get("adx", 0.0)),
    )
