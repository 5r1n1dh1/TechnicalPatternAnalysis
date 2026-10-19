"""Candlestick summary — last 5 sessions, reliability notes per book-spec §5."""
from __future__ import annotations
import numpy as np, pandas as pd


def summarize(candles: pd.DataFrame) -> list[dict]:
    results = []
    for i in range(max(0, len(candles)-5), len(candles)):
        o, h, l, c = candles.iloc[i]["open"], candles.iloc[i]["high"], candles.iloc[i]["low"], candles.iloc[i]["close"]
        prev = candles.iloc[i-1] if i > 0 else candles.iloc[i]
        body = abs(c - o)
        range_ = h - l
        upper_shadow = h - max(o, c)
        lower_shadow = min(o, c) - l
        # Patterns
        patterns = []
        # Engulfing
        if i > 0:
            po, pc = prev["open"], prev["close"]
            if (o < pc and c > po) or (o > pc and c < po):
                patterns.append({"pattern": "engulfing", "direction": "bullish" if c > o else "bearish"})
        # Piercing / Dark cloud (approx)
        if i > 0:
            if o < prev["close"] and c > (prev["open"] + prev["close"])/2:
                patterns.append({"pattern": "piercing", "strength": 6})
            if o > prev["close"] and c < (prev["open"] + prev["close"])/2:
                patterns.append({"pattern": "dark_cloud", "strength": 6})
        # Tweezer (approx)
        if i > 0:
            if abs(h - prev["high"]) < (h - l) * 0.1:
                patterns.append({"pattern": "tweezer", "strength": 5})
        # Hammer / Shooting star
        if lower_shadow > 2 * body and upper_shadow < body:
            patterns.append({"pattern": "hammer", "strength": 7})
        if upper_shadow > 2 * body and lower_shadow < body:
            patterns.append({"pattern": "shooting_star", "strength": 7})
        # Doji family
        if body < 0.05 * range_:
            if lower_shadow > 2 * upper_shadow:
                patterns.append({"pattern": "dragonfly_doji", "strength": 8})
            elif upper_shadow > 2 * lower_shadow:
                patterns.append({"pattern": "gravestone_doji", "strength": 8})
            else:
                patterns.append({"pattern": "long_legged_doji", "strength": 6})
        for p in patterns:
            p["date"] = str(candles.index[i]) if isinstance(candles.index[i], pd.Timestamp) else str(i)
            p["reliability"] = "location > pattern name"
            results.append(p)
    return results
