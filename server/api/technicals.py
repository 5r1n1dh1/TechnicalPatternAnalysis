"""Technicals endpoint — M4."""
from __future__ import annotations
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import pandas as pd, numpy as np
from server.core import indicators, trend, s_r, candlestick_summary, structure

router = APIRouter(tags=["technicals"])

class SRItem(BaseModel):
    price: float
    strength: int
    touches: int
    volume: float

class CandleChip(BaseModel):
    pattern: str
    date: str
    strength: int

class Verdict(BaseModel):
    bullish: int
    bearish: int
    neutral: int

class TechnicalResponse(BaseModel):
    rsi14: float
    macdHistNorm: float
    ema20: float
    ema50: float
    adx: float
    atr14pct: float
    bollingerWidthPct: float
    volRatio520: float
    deliveryPct5: Optional[float]
    s_r: dict
    candleChips: List[CandleChip]
    verdict: Verdict
    trendBadge: str
    structureChip: str

@router.get("/technicals")
def get_technicals(symbol: str = "RELIANCE") -> TechnicalResponse:
    # Stub: real implementation pulls 1y daily bars from cache/yahoo
    # Point-in-time computations only — no lookahead
    return TechnicalResponse(
        rsi14=58.0, macdHistNorm=-0.12, ema20=1425.0, ema50=1410.0, adx=28.0,
        atr14pct=2.3, bollingerWidthPct=0.8, volRatio520=1.2, deliveryPct5=0.68,
        s_r={"support": [{"price": 1420.0, "strength": 8, "touches": 5, "volume": 1.1e7}],
              "resistance": [{"price": 1445.0, "strength": 7, "touches": 6, "volume": 1.3e7}]},
        candleChips=[{"pattern": "bullish_engulfing", "date": "2026-10-10", "strength": 7}],
        verdict=Verdict(bullish=6, bearish=4, neutral=2),
        trendBadge="Uptrend", structureChip="UP"
    )
