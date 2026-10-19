"""Flag pattern detector (bull/bear flags)."""

from __future__ import annotations

from typing import List, Optional, Dict, Any

import pandas as pd
import numpy as np

from ..patterns import (
    SwingPivot,
    Detection,
    TrendValidityMixin,
    VolumeValidityMixin,
    calculate_confidence,
)
from ..structure import atr as compute_atr


class FlagDetector(TrendValidityMixin, VolumeValidityMixin):
    """Detects bullish and bearish flag patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        if len(pivots) < 4 or len(candles) < 20:
            return []

        pivots_sorted = sorted(pivots, key=lambda p: p.idx)
        detections = []

        # Look for pole + flag structure
        for i in range(len(pivots_sorted) - 3):
            p1, p2, p3, p4 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2], pivots_sorted[i+3]
            # Bullish flag: downtrend consolidation after up move? Or up pole, then flag down/sloping
            # Or simpler: strong move up, then retracement forming flag, then breakout
            # Check for up pole then down flag
            if p1.price < p2.price and p3.price < p2.price and p4.price > p3.price:  # pole up then flag down then breakout up
                pole_height = p2.price - p1.price
                flag_range = p2.price - p3.price  # retracement
                if pole_height > 0 and flag_range > 0 and flag_range < pole_height * 0.5:  # flag range < 1/2 impulse
                    if self.has_prior_trend(candles, 'bullish'):
                        detection = self._build_bull_flag([p1, p2, p3, p4], candles)
                        if detection:
                            detections.append(detection)

            # Bearish flag: pole down, flag up, breakout down
            if p1.price > p2.price and p3.price > p2.price and p4.price < p3.price:
                pole_height = p1.price - p2.price
                flag_range = p3.price - p2.price
                if pole_height > 0 and flag_range > 0 and flag_range < pole_height * 0.5:
                    if self.has_prior_trend(candles, 'bearish'):
                        detection = self._build_bear_flag([p1, p2, p3, p4], candles)
                        if detection:
                            detections.append(detection)

        return detections

    def _get_atr(self, candles: pd.DataFrame) -> float:
        if "atr" in candles.columns:
            try:
                return float(candles["atr"].iloc[-1])
            except Exception:
                pass
        try:
            high = candles["high"].values
            low = candles["low"].values
            close = candles["close"].values
            atr_vals = compute_atr(high, low, close, period=14)
            return float(atr_vals[-1]) if len(atr_vals) > 0 else 0.0
        except Exception:
            return 0.0

    def _build_bull_flag(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3, p4 = pivots[0], pivots[1], pivots[2], pivots[3]
        pole_height = p2.price - p1.price
        target_price = p4.price + pole_height
        atr_val = self._get_atr(candles)
        stop_price = p3.price - 0.5 * atr_val if atr_val > 0 else p3.price * 0.99
        risk = abs(p4.price - stop_price)
        reward = abs(target_price - p4.price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"flag_valid": 10.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=75.0)
        return Detection(
            pattern="flag",
            direction="bullish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p2.idx), "price": float(p2.price)}, {"t": str(p4.idx), "price": float(p4.price)}],
            neckline={"t": str(p3.idx), "price": float(p3.price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks below flag low"
        )

    def _build_bear_flag(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3, p4 = pivots[0], pivots[1], pivots[2], pivots[3]
        pole_height = p1.price - p2.price
        target_price = p4.price - pole_height
        atr_val = self._get_atr(candles)
        stop_price = p3.price + 0.5 * atr_val if atr_val > 0 else p3.price * 1.01
        risk = abs(stop_price - p4.price)
        reward = abs(p4.price - target_price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"flag_valid": 10.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=75.0)
        return Detection(
            pattern="flag",
            direction="bearish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p2.idx), "price": float(p2.price)}, {"t": str(p4.idx), "price": float(p4.price)}],
            neckline={"t": str(p3.idx), "price": float(p3.price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks above flag high"
        )
