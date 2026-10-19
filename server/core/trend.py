"""Trend rubric — DI direction × ADX band per book-spec §5.2."""
from __future__ import annotations
import numpy as np

def adx_band_factor(adx14: float) -> float:
    if adx14 < 20: return 0.0
    if adx14 < 25: return 0.5
    if adx14 <= 50: return 1.0
    return 1.5

def trend_from_series(close: np.ndarray, ema20: np.ndarray, ema50: np.ndarray, plus_di: float, minus_di: float, adx14: float) -> dict:
    s_ema20 = np.sign(ema20[-1] - ema20[-2]) if len(ema20) > 1 else 1.0
    s_ema50 = np.sign(ema50[-1] - ema50[-2]) if len(ema50) > 1 else 1.0
    score = (0.35 * s_ema20 + 0.25 * s_ema50 + 0.25 * np.sign(close[-1] - ema50[-1]) +
             0.15 * np.sign(plus_di - minus_di) * adx_band_factor(adx14))
    return {"label": trend_badge(score), "strength": trend_strength(score), "score": float(score)}

def trend_badge(score: float) -> str:
    if score > 0.35: return "Uptrend"
    if score < -0.35: return "Downtrend"
    return "Sideways"

def trend_strength(score: float) -> float:
    return min(100.0, abs(score) * 100)
