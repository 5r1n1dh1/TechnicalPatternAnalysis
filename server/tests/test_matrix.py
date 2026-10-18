"""Validity-matrix tests: every declared combo passes, everything else is 400 invalidCombo."""

import pytest
from fastapi.testclient import TestClient

from api.index import app
from core.constants import INTERVALS, RANGES, VALID_COMBOS

ALL_COMBOS = {(iv, rg) for iv in INTERVALS for rg in RANGES}


def test_matrix_declared_combos_pass() -> None:
    client = TestClient(app)
    for interval, ranges in VALID_COMBOS.items():
        for rg in ranges:
            # validate_combo raises ApiError on failure; absence of raise = pass.
            from core.constants import validate_combo

            validate_combo(interval, rg)


def test_every_invalid_combo_raises_400() -> None:
    from api.errors import ApiError
    from core.constants import validate_combo

    invalid = ALL_COMBOS - {(iv, rg) for iv, rs in VALID_COMBOS.items() for rg in rs}
    assert invalid, "matrix must reject some combos"
    for interval, rg in invalid:
        with pytest.raises(ApiError) as exc:
            validate_combo(interval, rg)
        assert exc.value.status == 400
        assert exc.value.payload.error.code == "invalidCombo"


def test_unknown_interval_or_range_rejected() -> None:
    from api.errors import ApiError
    from core.constants import validate_combo

    with pytest.raises(ApiError):
        validate_combo("2m", "1D")
    with pytest.raises(ApiError):
        validate_combo("1d", "3D")
