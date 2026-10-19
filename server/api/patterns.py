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


class Anchor(BaseModel):
    t: str
    price: float


class Neckline(BaseModel):
    t: str
    price: float


class Detection(BaseModel):
    pattern: str
    direction: str
    confidence: int
    anchors: List[Anchor]
    neckline: Neckline


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
    return PatternsResponse(detections=[])
