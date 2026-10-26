"""API-level tests for /patterns endpoint (T-37)."""
import pytest
from fastapi.testclient import TestClient
from api.patterns import router, Detection, Pivot

client = TestClient(router)

POSITIVE_SYMBOL = "TEST"


def fixture_df(name):
    import os, pandas as pd
    return pd.read_csv(os.path.join("tests/fixtures/patterns", name))


@pytest.mark.parametrize("fname", [
    "hs_pos.csv", "ihs_pos.csv", "cup_pos.csv", "double_top_pos.csv",
    "double_bottom_pos.csv", "triple_top_pos.csv", "triple_bottom_pos.csv",
    "bull_flag_pos.csv", "bear_flag_pos.csv",
])
def test_positive_fixtures_api(fname, monkeypatch):
    import core.patterns as cp
    df = fixture_df(fname)
    monkeypatch.setattr(cp, "detect_patterns", lambda c, **kw: [
        cp.Detection(
            pattern="head_and_shoulders", direction="bearish", confidence=80,
            anchors=[{"t":"0","price":100}, {"t":"5","price":110}, {"t":"10","price":100}],
            neckline={"t":"2","price":105},
            pivot_idx=[(0,100,True),(2,105,False),(5,110,True),(7,108,False),(10,100,True)]
        )
    ])
    # Mock load_bars to return fixture data with 'date' column mapped
    import core.bars as bars_mod
    def mock_load(symbol, interval, range_str):
        df2 = df.copy()
        df2["date"] = df2.index.map(lambda i: f"2023-01-{i+1:02d}T10:00:00Z")
        return df2.rename(columns={"open":"o","high":"h","low":"l","close":"c","volume":"v"})
    monkeypatch.setattr(bars_mod, "load_bars", mock_load)
    resp = client.get("/patterns", params={"symbol": POSITIVE_SYMBOL, "interval": "1d", "range": "1Y"})
    assert resp.status_code == 200
    data = resp.json()
    assert len(data["detections"]) >= 1
    det = data["detections"][0]
    for a in det["anchors"]:
        assert "t" in a and isinstance(a["t"], str)
        assert a["t"].endswith("Z")
    pivots = det.get("pivots", [])
    for p in pivots:
        assert p["kind"] in {"HIGH", "LOW"}


def test_negative_fixtures_api(monkeypatch):
    import core.patterns as cp
    import core.bars as bars_mod
    for fname in ["hs_neg.csv", "cup_neg.csv", "double_neg.csv", "flag_neg.csv", "noisy_flat.csv"]:
        df = fixture_df(fname)
        monkeypatch.setattr(cp, "detect_patterns", lambda c, **kw: [])
        def mock_load(symbol, interval, range_str):
            df2 = df.copy()
            df2["date"] = df2.index.map(lambda i: f"2023-01-{i+1:02d}T10:00:00Z")
            return df2.rename(columns={"open":"o","high":"h","low":"l","close":"c","volume":"v"})
        monkeypatch.setattr(bars_mod, "load_bars", mock_load)
        resp = client.get("/patterns", params={"symbol": POSITIVE_SYMBOL, "interval": "1d", "range": "1Y"})
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["detections"]) == 0


def test_pattern_filter(monkeypatch):
    import core.patterns as cp
    import core.bars as bars_mod
    df = fixture_df("hs_pos.csv")
    monkeypatch.setattr(cp, "detect_patterns", lambda c, pattern=None, **kw: [
        cp.Detection(pattern="head_and_shoulders", direction="bearish", confidence=75,
                     anchors=[{"t":"0","price":100}], neckline={"t":"1","price":105},
                     pivot_idx=[(0,100,True),(1,105,False)])
    ] if pattern == "head_and_shoulders" else [])
    def mock_load(symbol, interval, range_str):
        df2 = df.copy()
        df2["date"] = df2.index.map(lambda i: f"2023-01-{i+1:02d}T10:00:00Z")
        return df2.rename(columns={"open":"o","high":"h","low":"l","close":"c","volume":"v"})
    monkeypatch.setattr(bars_mod, "load_bars", mock_load)
    resp_all = client.get("/patterns", params={"symbol": POSITIVE_SYMBOL, "interval": "1d", "range":"1Y"})
    resp_filter = client.get("/patterns", params={"symbol": POSITIVE_SYMBOL, "interval":"1d", "range":"1Y", "pattern":"head_and_shoulders"})
    assert resp_filter.status_code == 200
    assert len(resp_filter.json()["detections"]) >= 1


def test_unknown_symbol(monkeypatch):
    import core.bars as bars_mod
    monkeypatch.setattr(bars_mod, "load_bars", lambda *a, **kw: None)
    resp = client.get("/patterns", params={"symbol":"BAD", "interval":"1d", "range":"1Y"})
    assert resp.status_code == 404
    assert resp.json()["detail"] == "unknownSymbol"


def test_invalid_interval_range(monkeypatch):
    resp = client.get("/patterns", params={"symbol":"X", "interval":"bad", "range":"1Y"})
    assert resp.status_code == 400
    assert resp.json()["detail"] == "invalidCombo"
