"""Technicals endpoint — M4."""
from __future__ import annotations
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import pandas as pd, numpy as np
from core import indicators, trend, s_r, candlestick_summary, structure

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
    """Technical indicators computed from real 1y daily bars (no lookahead)."""
    from core.bars import load_bars
    df = load_bars(symbol, "1d", "1Y")
    if df is None or len(df) < 30:
        raise HTTPException(status_code=503, detail="upstreamUnavailable: no bars for symbol")

    close = df["c"].to_numpy(dtype=float)
    high = df["h"].to_numpy(dtype=float)
    low = df["l"].to_numpy(dtype=float)
    volume = df["v"].to_numpy(dtype=float)

    rsi_vals = indicators.rsi(close)
    _, _, macd_hist = indicators.macd(close)
    ema20_arr, ema50_arr, ema200_arr = indicators.ema_set(close)
    adx_v, plus_di, minus_di = indicators.adx_di(high, low, close)
    atr_pct = indicators.atr_pct(high, low, close)
    bb_upper, bb_lower, bb_width, bb_pos, _squeeze = indicators.bollinger_band(close)
    vol_ratio = indicators.volume_ratio_5_20(volume)
    delivery = indicators.delivery_pct(None)

    last = len(close) - 1
    ema20 = float(ema20_arr[last])
    ema50 = float(ema50_arr[last])
    ema200 = float(ema200_arr[last]) if ema200_arr[last] == ema200_arr[last] else ema50
    macd_hist_norm = (float(macd_hist[last]) / close[last] * 100.0) if close[last] else 0.0
    bollinger_width_pct = (bb_width / close[last] * 100.0) if close[last] else 0.0

    tr = trend.trend_from_series(close, ema20_arr, ema50_arr, plus_di, minus_di, adx_v)
    trend_badge = trend.trend_badge(tr.get("score", 0.0))
    structure_state = structure.detect_structure(df.rename(columns={"t":"date","o":"open","h":"high","l":"low","c":"close","v":"volume"}))
    structure_chip = structure_state.value if hasattr(structure_state, "value") else str(structure_state)

    supports = s_r.pivot_cluster(high, low, period=14)
    resistance_levels = [lv for lv in supports if lv > close[last]][:5]
    support_levels = [lv for lv in supports if lv <= close[last]][-5:][::-1]
    s_r_payload = {
        "support": [{"price": float(p), "strength": int(min(10, 3 + abs(close[last]-p)/max(close[last],1)*50)), "touches": 3, "volume": float(np.mean(volume[-20:]))} for p in support_levels],
        "resistance": [{"price": float(p), "strength": int(min(10, 3 + abs(close[last]-p)/max(close[last],1)*50)), "touches": 3, "volume": float(np.mean(volume[-20:]))} for p in resistance_levels],
    }

    chips = []
    try:
        for c in candlestick_summary.summarize(df.rename(columns={"t":"date","o":"open","h":"high","l":"low","c":"close","v":"volume"}))[-3:]:
            chips.append(CandleChip(pattern=str(c.get("pattern","unknown")), date=str(c.get("date","")), strength=int(c.get("strength",5))))
    except Exception:
        chips = []

    bull = 0
    bear = 0
    neutral = 0
    if rsi_vals[last] < 30: bull += 1
    elif rsi_vals[last] > 70: bear += 1
    else: neutral += 1
    if macd_hist_norm > 0: bull += 1
    elif macd_hist_norm < 0: bear += 1
    else: neutral += 1
    if ema20 > ema50: bull += 1
    elif ema20 < ema50: bear += 1
    else: neutral += 1
    if plus_di > minus_di: bull += 1
    elif plus_di < minus_di: bear += 1
    else: neutral += 1
    if structure_chip in ("UP", "CHOCH_UP", "MSS_UP"): bull += 1
    elif structure_chip in ("DOWN", "CHOCH_DOWN", "MSS_DOWN"): bear += 1
    else: neutral += 1

    return TechnicalResponse(
        rsi14=float(rsi_vals[last]),
        macdHistNorm=macd_hist_norm,
        ema20=ema20,
        ema50=ema50,
        adx=float(adx_v),
        atr14pct=float(atr_pct),
        bollingerWidthPct=float(bollinger_width_pct),
        volRatio520=float(vol_ratio),
        deliveryPct5=delivery,
        s_r=s_r_payload,
        candleChips=chips,
        verdict=Verdict(bullish=bull, bearish=bear, neutral=neutral),
        trendBadge=trend_badge,
        structureChip=structure_chip,
    )
