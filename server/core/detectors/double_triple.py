"""Double and Triple top/bottom pattern detector."""

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


class DoubleTripleDetector(TrendValidityMixin, VolumeValidityMixin):
    """Detects double and triple top/bottom patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        if len(pivots) < 4:
            return []

        pivots_sorted = sorted(pivots, key=lambda p: p.idx)
        detections = []

        # Double top: high, low, high (2 peaks)
        for i in range(len(pivots_sorted) - 2):
            p1, p2, p3 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2]
            if p1.is_high and not p2.is_high and p3.is_high:
                if p1.price > p2.price and p3.price > p2.price:
                    peak_diff = abs(p1.price - p3.price) / max(p1.price, p3.price)
                    if peak_diff < 0.08:  # Peaks within 8%
                        if self.has_prior_trend(candles, 'bearish'):
                            detection = self._build_double_top([p1, p2, p3], candles)
                            if detection:
                                detections.append(detection)

        # Double bottom: low, high, low
        for i in range(len(pivots_sorted) - 2):
            p1, p2, p3 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2]
            if not p1.is_high and p2.is_high and not p3.is_high:
                if p1.price < p2.price and p3.price < p2.price:
                    trough_diff = abs(p1.price - p3.price) / max(p1.price, p3.price)
                    if trough_diff < 0.08:
                        if self.has_prior_trend(candles, 'bullish'):
                            detection = self._build_double_bottom([p1, p2, p3], candles)
                            if detection:
                                detections.append(detection)

        # Triple top: high, low, high, low, high
        for i in range(len(pivots_sorted) - 4):
            p1, p2, p3, p4, p5 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2], pivots_sorted[i+3], pivots_sorted[i+4]
            if p1.is_high and not p2.is_high and p3.is_high and not p4.is_high and p5.is_high:
                if p1.price > p2.price and p3.price > p2.price and p3.price > p4.price and p5.price > p4.price:
                    peak_diff1 = abs(p1.price - p3.price) / max(p1.price, p3.price)
                    peak_diff2 = abs(p3.price - p5.price) / max(p3.price, p5.price)
                    if peak_diff1 < 0.1 and peak_diff2 < 0.1:
                        if self.has_prior_trend(candles, 'bearish'):
                            detection = self._build_triple_top([p1, p2, p3, p4, p5], candles)
                            if detection:
                                detections.append(detection)

        # Triple bottom: low, high, low, high, low
        for i in range(len(pivots_sorted) - 4):
            p1, p2, p3, p4, p5 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2], pivots_sorted[i+3], pivots_sorted[i+4]
            if not p1.is_high and p2.is_high and not p3.is_high and p4.is_high and not p5.is_high:
                if p1.price < p2.price and p3.price < p2.price and p3.price < p4.price and p5.price < p4.price:
                    trough_diff1 = abs(p1.price - p3.price) / max(p1.price, p3.price)
                    trough_diff2 = abs(p3.price - p5.price) / max(p3.price, p5.price)
                    if trough_diff1 < 0.1 and trough_diff2 < 0.1:
                        if self.has_prior_trend(candles, 'bullish'):
                            detection = self._build_triple_bottom([p1, p2, p3, p4, p5], candles)
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

    def _build_double_top(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3 = pivots[0], pivots[1], pivots[2]
        neckline_price = p2.price  # trough between peaks
        target_price = neckline_price - (p1.price - neckline_price)
        atr_val = self._get_atr(candles)
        stop_price = max(p1.price, p3.price) + 0.5 * atr_val if atr_val > 0 else max(p1.price, p3.price) * 1.01
        risk = abs(stop_price - max(p1.price, p3.price))
        reward = abs(max(p1.price, p3.price) - target_price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"peak_level": 5.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=70.0)
        return Detection(
            pattern="double_triple",
            direction="bearish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p3.idx), "price": float(p3.price)}],
            neckline={"t": str(p2.idx), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks above the double top peaks"
        )

    def _build_double_bottom(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3 = pivots[0], pivots[1], pivots[2]
        neckline_price = p2.price
        target_price = neckline_price + (neckline_price - p1.price)
        atr_val = self._get_atr(candles)
        stop_price = min(p1.price, p3.price) - 0.5 * atr_val if atr_val > 0 else min(p1.price, p3.price) * 0.99
        risk = abs(neckline_price - stop_price)
        reward = abs(target_price - neckline_price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"trough_level": 5.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=70.0)
        return Detection(
            pattern="double_triple",
            direction="bullish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p3.idx), "price": float(p3.price)}],
            neckline={"t": str(p2.idx), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks below the double bottom troughs"
        )

    def _build_triple_top(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3, p4, p5 = pivots[0], pivots[1], pivots[2], pivots[3], pivots[4]
        neckline_price = min(p2.price, p4.price)
        target_price = neckline_price - (p3.price - neckline_price)
        atr_val = self._get_atr(candles)
        stop_price = p3.price + 0.5 * atr_val if atr_val > 0 else p3.price * 1.01
        risk = abs(stop_price - p3.price)
        reward = abs(p3.price - target_price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"triple": 10.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=75.0)
        return Detection(
            pattern="double_triple",
            direction="bearish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p3.idx), "price": float(p3.price)}, {"t": str(p5.idx), "price": float(p5.price)}],
            neckline={"t": str(min(p2.idx, p4.idx)), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks above the triple top peaks"
        )

    def _build_triple_bottom(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        p1, p2, p3, p4, p5 = pivots[0], pivots[1], pivots[2], pivots[3], pivots[4]
        neckline_price = max(p2.price, p4.price)
        target_price = neckline_price + (neckline_price - p3.price)
        atr_val = self._get_atr(candles)
        stop_price = p3.price - 0.5 * atr_val if atr_val > 0 else p3.price * 0.99
        risk = abs(p3.price - stop_price)
        reward = abs(target_price - p3.price)
        rr = reward / risk if risk > 0 else 0.0
        factors = {"triple": 10.0}
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=75.0)
        return Detection(
            pattern="double_triple",
            direction="bullish",
            confidence=confidence,
            anchors=[{"t": str(p1.idx), "price": float(p1.price)}, {"t": str(p3.idx), "price": float(p3.price)}, {"t": str(p5.idx), "price": float(p5.price)}],
            neckline={"t": str(max(p2.idx, p4.idx)), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks below the triple bottom troughs"
        )
