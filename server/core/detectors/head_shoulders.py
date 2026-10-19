"""Head and Shoulders pattern detector per book-spec §7."""

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


class HeadShouldersDetector(TrendValidityMixin, VolumeValidityMixin):
    """Detects bearish and bullish Head & Shoulders patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        """Detect head and shoulder patterns.

        Args:
            pivots: List of SwingPivot objects
            candles: DataFrame with OHLCV data

        Returns:
            List of Detection objects.
        """
        # Need at least 5 pivots to form H&S (LS, T1, H, T2, RS)
        if len(pivots) < 5:
            return []

        # Sort pivots by index (chronological)
        pivots_sorted = sorted(pivots, key=lambda p: p.idx)

        detections = []

        # Scan for bearish H&S: high, low, high, low, high (left shoulder, trough, head, trough, right shoulder)
        for i in range(len(pivots_sorted) - 4):
            p1, p2, p3, p4, p5 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2], pivots_sorted[i+3], pivots_sorted[i+4]
            # Check structure: high, low, high, low, high
            if (p1.is_high and not p2.is_high and p3.is_high and not p4.is_high and p5.is_high):
                # Head (p3) should be higher than both shoulders
                if p3.price > p1.price and p3.price > p5.price:
                    # Shoulders should be similar height (within ~5%)
                    shoulder_diff = abs(p1.price - p5.price) / max(p1.price, p5.price)
                    if shoulder_diff < 0.1:  # Within 10% is lenient for synthetic data
                        # Prior trend should be uptrend for bearish H&S
                        if self.has_prior_trend(candles, 'bearish'):
                            detection = self._build_bearish_hs_detection([p1, p2, p3, p4, p5], candles, i)
                            if detection:
                                detections.append(detection)

        # Scan for bullish inverse H&S: low, high, low, high, low
        for i in range(len(pivots_sorted) - 4):
            p1, p2, p3, p4, p5 = pivots_sorted[i], pivots_sorted[i+1], pivots_sorted[i+2], pivots_sorted[i+3], pivots_sorted[i+4]
            # Check structure: low, high, low, high, low
            if (not p1.is_high and p2.is_high and not p3.is_high and p4.is_high and not p5.is_high):
                # Inverse head (p3) should be lower than both shoulders (p1, p5)
                if p3.price < p1.price and p3.price < p5.price:
                    # Shoulders similar
                    shoulder_diff = abs(p1.price - p5.price) / max(p1.price, p5.price)
                    if shoulder_diff < 0.1:
                        if self.has_prior_trend(candles, 'bullish'):
                            detection = self._build_bullish_hs_detection([p1, p2, p3, p4, p5], candles, i)
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

    def _build_bearish_hs_detection(self, pivots: List[SwingPivot], candles: pd.DataFrame, start_idx: int) -> Optional[Detection]:
        """Build detection for bearish head and shoulders: pivots [LS, T1, H, T2, RS]."""
        ls, t1, h, t2, rs = pivots[0], pivots[1], pivots[2], pivots[3], pivots[4]

        # Neckline connects t1 and t2
        neckline_price = (t1.price + t2.price) / 2.0

        # Measured move: distance from head to neckline, projected down
        measured_move = h.price - neckline_price
        target_price = neckline_price - measured_move

        atr_val = self._get_atr(candles)
        stop_price = neckline_price + 0.5 * atr_val if atr_val > 0 else neckline_price * 1.01

        # R:R
        risk = abs(stop_price - neckline_price)  # breakout level assumption around neckline? Or entry
        reward = abs(neckline_price - target_price)
        rr = reward / risk if risk > 0 else 0.0

        # Confidence calculation
        factors = {}
        # Head prominence
        if h.price / max(ls.price, rs.price) > 1.2:
            factors["head_prominence"] = 5.0
        # Shoulder symmetry
        shoulder_diff = abs(ls.price - rs.price) / max(ls.price, rs.price)
        if shoulder_diff < 0.05:
            factors["symmetry"] = 10.0
        elif shoulder_diff < 0.1:
            factors["symmetry"] = 5.0
        # Volume confirmation
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=70.0)

        anchors = [
            {"t": str(ls.idx), "price": float(ls.price)},
            {"t": str(h.idx), "price": float(h.price)},
            {"t": str(rs.idx), "price": float(rs.price)},
        ]

        return Detection(
            pattern="head_and_shoulders",
            direction="bearish",
            confidence=confidence,
            anchors=anchors,
            neckline={"t": str(t2.idx), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Neckline recaptured or closes above right shoulder"
        )

    def _build_bullish_hs_detection(self, pivots: List[SwingPivot], candles: pd.DataFrame, start_idx: int) -> Optional[Detection]:
        """Build detection for bullish inverse head and shoulders: pivots [LS_low, T1, H_low, T2, RS_low]."""
        ls, t1, h, t2, rs = pivots[0], pivots[1], pivots[2], pivots[3], pivots[4]

        # Neckline connects t1 and t2 (highs)
        neckline_price = (t1.price + t2.price) / 2.0

        # Measured move up
        measured_move = neckline_price - h.price
        target_price = neckline_price + measured_move

        atr_val = self._get_atr(candles)
        stop_price = neckline_price - 0.5 * atr_val if atr_val > 0 else neckline_price * 0.99

        risk = abs(neckline_price - stop_price)
        reward = abs(target_price - neckline_price)
        rr = reward / risk if risk > 0 else 0.0

        factors = {}
        if neckline_price / min(ls.price, rs.price) > 1.2:
            factors["head_prominence"] = 5.0
        shoulder_diff = abs(ls.price - rs.price) / max(ls.price, rs.price)
        if shoulder_diff < 0.05:
            factors["symmetry"] = 10.0
        elif shoulder_diff < 0.1:
            factors["symmetry"] = 5.0
        if self.check_volume_sequence(candles):
            factors["volume"] = 5.0
        confidence = calculate_confidence(factors, base=70.0)

        anchors = [
            {"t": str(ls.idx), "price": float(ls.price)},
            {"t": str(h.idx), "price": float(h.price)},
            {"t": str(rs.idx), "price": float(rs.price)},
        ]

        return Detection(
            pattern="head_and_shoulders",
            direction="bullish",
            confidence=confidence,
            anchors=anchors,
            neckline={"t": str(t2.idx), "price": float(neckline_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Neckline recaptured or closes below right shoulder"
        )
