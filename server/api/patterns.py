"""Patterns endpoint for chart pattern detection."""
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import List, Optional
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


class Pattern(str, Enum):
    head_and_shoulders = "head_and_shoulders"
    cup_and_handle = "cup_and_handle"
    double_triple = "double_triple"
    flag = "flag"


class Pivot(BaseModel):
    idx: int
    price: float
    kind: str


class Anchor(BaseModel):
    t: str
    price: float


class Neckline(BaseModel):
    t: Optional[str]
    price: Optional[float]


class Detection(BaseModel):
    pattern: str
    direction: str
    confidence: float
    anchors: List[Anchor]
    neckline: Optional[Neckline] = None
    pivots: List[Pivot] = []
    target_price: Optional[float] = None
    stop_price: Optional[float] = None
    rr: Optional[float] = None


class PatternsResponse(BaseModel):
    detections: List[Detection]


@router.get("/patterns", response_model=PatternsResponse)
def get_patterns(
    symbol: str = Query(..., description="Stock symbol"),
    interval: Interval = Query(..., description="Time interval"),
    range: Range = Query(..., description="Data range"),
    pattern: Optional[Pattern] = Query(None, description="Specific pattern"),
):
    """Detect chart patterns over a range."""
    try:
        from core.bars import load_bars
    except Exception:
        from api.core.bars import load_bars
    try:
        import core.patterns as cp
    except Exception:
        import api.core.patterns as cp
    import pandas as pd

    # Validate interval/range combo
    allowed_intervals = {"5m", "15m", "30m", "1h", "1d", "1w", "1M"}
    allowed_ranges = {"1D", "5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y"}
    if interval.value not in allowed_intervals or range.value not in allowed_ranges:
        raise HTTPException(status_code=400, detail="invalidCombo")

    df = load_bars(symbol, interval.value, range.value)
    if df is None or df.empty:
        raise HTTPException(status_code=404, detail="unknownSymbol")

    # Ensure required columns exist
    for col in ("high", "low", "close"):
        if col in df.columns:
            continue
        for alias in ("h", "l", "c", "High", "Low", "Close"):
            if alias in df.columns:
                df = df.rename(columns={alias: col})
                break
    if "high" not in df.columns or "low" not in df.columns or "close" not in df.columns:
        raise HTTPException(status_code=404, detail="unknownSymbol")

    if "date" not in df.columns and "t" in df.columns:
        df = df.rename(columns={"t": "date"})

    detections = cp.detect_patterns(df, pattern=pattern.value if pattern else None)
    payload = cp.build_detection_payload(df, detections)

    results = []
    for entry in payload:
        pivots = [Pivot(idx=p["idx"], price=p["price"], kind=p["kind"]) for p in entry.get("pivot_idx", [])]
        results.append(Detection(
            pattern=entry["pattern"],
            direction=entry["direction"],
            confidence=int(entry.get("confidence", 0)),
            anchors=[Anchor(t=a["t"], price=a["price"]) for a in entry["anchors"]],
            neckline=Neckline(t=entry["neckline"]["t"], price=entry["neckline"]["price"]) if entry.get("neckline") else None,
            pivots=pivots,
            target_price=entry.get("target_price"),
            stop_price=entry.get("stop_price"),
            rr=entry.get("rr"),
        ))
    return PatternsResponse(detections=results)
