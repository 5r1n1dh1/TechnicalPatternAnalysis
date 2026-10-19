"""S/R levels — pivot clustering, floor pivots, ranking per book-spec §5.3."""
from __future__ import annotations
import numpy as np, pandas as pd


def pivot_cluster(high: np.ndarray, low: np.ndarray, period: int = 14) -> list[float]:
    # Simple pivot detection using rolling max/min within window
    pivots = []
    for i in range(period, len(high) - period):
        if high[i] == np.max(high[i-period:i+period+1]):
            pivots.append(float(high[i]))
        if low[i] == np.min(low[i-period:i+period+1]):
            pivots.append(float(low[i]))
    # Deduplicate and sort
    pivots = sorted(set(pivots))
    # Cluster merge within 0.75 * ATR
    if not pivots:
        return pivots
    atr_est = float(np.mean(high[-14:] - low[-14:]))
    clusters = [pivots[0]]
    for p in pivots[1:]:
        if abs(p - clusters[-1]) <= 0.75 * atr_est:
            clusters[-1] = (clusters[-1] + p) / 2
        else:
            clusters.append(p)
    return [float(c) for c in clusters]


def floor_pivots(ohlc_df: pd.DataFrame) -> dict:
    last = ohlc_df.iloc[-1]
    h, l, c = float(last["high"]), float(last["low"]), float(last["close"])
    pp = (h + l + c) / 3
    r1 = 2 * pp - l
    s1 = 2 * pp - h
    r2 = pp + (h - l)
    s2 = pp - (h - l)
    r3 = r2 + (h - l)
    s3 = s2 - (h - l)
    return {"PP": pp, "R1": r1, "R2": r2, "R3": r3, "S1": s1, "S2": s2, "S3": s3}


def rank_sr(levels: list[float], touches: int = 1, recency: float = 1.0, volume_at_touch: float = 1.0) -> float:
    return float(touches * recency * (volume_at_touch / 1e6 + 1))
