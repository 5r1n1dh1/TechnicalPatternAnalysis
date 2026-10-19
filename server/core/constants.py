"""Shared constants: API enums, interval/range validity matrix, market config (C13, C24)."""

from typing import Final, FrozenSet, Mapping

from api.errors import ApiError, invalid_combo

INTERVALS: Final[FrozenSet[str]] = frozenset({"5m", "15m", "30m", "1h", "1d", "1w", "1M"})
RANGES: Final[FrozenSet[str]] = frozenset({"1D", "5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y"})

# interval -> ranges allowed (plan §2.2 validity matrix; "—" cells excluded).
VALID_COMBOS: Final[Mapping[str, FrozenSet[str]]] = {
    "5m": frozenset({"1D", "5D", "1M"}),
    "15m": frozenset({"1D", "5D", "1M"}),
    "30m": frozenset({"1D", "5D", "1M"}),
    "1h": frozenset({"1D", "5D", "1M", "3M", "6M", "1Y"}),
    "1d": frozenset({"5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y"}),
    "1w": frozenset({"1Y", "2Y", "5Y", "10Y"}),
    "1M": frozenset({"1Y", "2Y", "5Y", "10Y"}),
}

TIMEZONE: Final = "Asia/Kolkata"
SYMBOL_SUFFIX: Final = ".NS"

# C24 — valuation / WACC / macro constants (locked; §13.2 #6 halt if changed)
G_SEC_10Y: Final[float] = 7.0
ERP: Final[float] = 6.0
TAX_RATE: Final[float] = 0.25
WACC_BULL_G: Final[float] = 0.05
WACC_BASE_G: Final[float] = 0.04
WACC_BEAR_G: Final[float] = 0.025

# Quality tiers (§6, §4) — never change without halt.
MO_QUALITY_TIERS: Final[dict] = {
    "tier1": {"roce_min": 20.0, "cfo_pat_min": 0.8, "mos_min": 0.15, "mos_max": 0.20},
    "tier2": {"roce_min": 10.0, "cfo_pat_min": 0.5, "mos_min": 0.20, "mos_max": 0.30},
    "tier3": {"roce_min": 5.0,  "cfo_pat_min": 0.3, "mos_min": 0.30, "mos_max": 0.40},
    "cyclical": {"roce_min": 0.0,  "cfo_pat_min": 0.0, "mos_min": 0.40, "mos_max": 0.50},
}


def validate_combo(interval: str, range_key: str) -> None:
    """Raise the 400 invalidCombo error model unless the pair is in the matrix."""
    if interval not in INTERVALS or range_key not in RANGES:
        raise invalid_combo(interval, range_key)
    if range_key not in VALID_COMBOS[interval]:
        raise invalid_combo(interval, range_key)
