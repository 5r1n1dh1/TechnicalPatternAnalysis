"""Cup & Handle pattern detector per book-spec §7."""

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


class CupHandleDetector(TrendValidityMixin, VolumeValidityMixin):
    """Detects Cup & Handle patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        """Detect cup and handle patterns."""
        if len(pivots) < 5 or len(candles) < 20:
            return []

        # Find low pivots to fit quadratic
        low_pivots = [p for p in pivots if not p.is_high]
        if len(low_pivots) < 5:
            return []

        # Fit quadratic to low pivots (cup shape)
        x = np.array([p.idx for p in low_pivots])
        y = np.array([p.price for p in low_pivots])

        try:
            coeffs = np.polyfit(x, y, 2)
            a, b, c = coeffs
            y_pred = np.polyval(coeffs, x)
            ss_res = np.sum((y - y_pred) ** 2)
            ss_tot = np.sum((y - np.mean(y)) ** 2)
            r_squared = 1 - (ss_res / ss_tot) if ss_tot > 0 else 0
        except Exception:
            return []

        # Cup must have R^2 >= 0.8 and be U-shaped (a > 0)
        if r_squared < 0.8 or a <= 0:
            return []

        # Cup depth: difference between rim high and cup low
        rim_price = y.max()
        cup_low = y.min()
        cup_depth = rim_price - cup_low

        # Handle: drift <= 1/3 cup depth below rim
        handle_depth_threshold = cup_depth / 3.0
        handle_low = float(candles["low"].min())
        handle_depth = rim_price - handle_low

        if handle_depth > handle_depth_threshold:
            return []

        # Breakout above rim with volume
        breakout_price = float(candles["close"].iloc[-1])
        if breakout_price < rim_price:
            return []

        if self.check_volume_sequence(candles):
            factors = {"r2": 10.0, "cup_shape": 5.0, "volume": 5.0}
        else:
            factors = {"r2": 10.0, "cup_shape": 5.0}

        target_price = rim_price + cup_depth
        atr_val = self._get_atr(candles)
        stop_price = handle_low - 0.5 * atr_val if atr_val > 0 else handle_low * 0.99
        risk = abs(breakout_price - stop_price)
        reward = abs(target_price - breakout_price)
        rr = reward / risk if risk > 0 else 0.0

        confidence = calculate_confidence(factors, base=70.0)

        return [Detection(
            pattern="cup_and_handle",
            direction="bullish",
            confidence=confidence,
            anchors=[{
                "t": str(int(low_pivots[0].idx)),
                "price": float(low_pivots[0].price)
            }, {
                "t": str(int(low_pivots[-1].idx)),
                "price": float(low_pivots[-1].price)
            }],
            neckline={"t": str(int(low_pivots[len(low_pivots)//2].idx)), "price": float(rim_price)},
            target_price=float(target_price),
            stop_price=float(stop_price),
            rr=float(rr),
            invalidation_note="Price breaks below handle low"
        )]

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
