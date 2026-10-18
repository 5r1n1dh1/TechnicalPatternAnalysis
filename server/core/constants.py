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


def validate_combo(interval: str, range_key: str) -> None:
    """Raise the 400 invalidCombo error model unless the pair is in the matrix."""
    if interval not in INTERVALS or range_key not in RANGES:
        raise invalid_combo(interval, range_key)
    if range_key not in VALID_COMBOS[interval]:
        raise invalid_combo(interval, range_key)
