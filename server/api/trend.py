"""Trend endpoint."""
import json
from pathlib import Path
from fastapi import APIRouter, Query
from pydantic import BaseModel
from enum import Enum

router = APIRouter()

_DATA_DIR = Path(__file__).resolve().parents[1] / "data"
_KNOWN_SYMBOLS: set[str] = set()
try:
    if _DATA_DIR / "symbols.json".exists():
        raw = json.loads((_DATA_DIR / "symbols.json").read_text(encoding="utf-8"))
        _KNOWN_SYMBOLS = {str(entry["symbol"]) for entry in raw if isinstance(entry, dict)}
except Exception:
    _KNOWN_SYMBOLS = set()


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
    symbol: str
    interval: Interval
    range: Range
    label: str
    strength: float
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
    from api.errors import ApiError, ErrorResponse, ErrorBody
    import numpy as np
    df = load_bars(symbol, interval, range)
    if df is None or len(df) < 30:
        if symbol not in _KNOWN_SYMBOLS:
            raise ApiError(
                404,
                ErrorResponse(
                    error=ErrorBody(code="unknownSymbol", message=f"Unknown symbol: {symbol}")
                ),
            )
        raise ApiError(
            503,
            ErrorResponse(error=ErrorBody(code="upstreamUnavailable", message="Data unavailable, try after today's EOD sync")),
        )
    close = df["c"].to_numpy(dtype=float)
    high = df["h"].to_numpy(dtype=float)
    low = df["l"].to_numpy(dtype=float)
    ema20_arr, ema50_arr, _ = ema_set(close)
    adx_v, plus_di, minus_di = adx_di(high, low, close)
    tr = trend_from_series(close, ema20_arr, ema50_arr, float(plus_di), float(minus_di), float(adx_v))
    label = tr.get("label") or "Sideways"
    strength = float(trend_strength(tr.get("score", 50.0)))
    return TrendResponse(
        symbol=symbol,
        interval=interval,
        range=range,
        label=label,
        strength=strength,
        ema20=float(ema20_arr[-1]),
        ema50=float(ema50_arr[-1]),
        adx=float(adx_v),
    )
