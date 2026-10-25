"""Trend endpoint truth tests (T-36)."""
import pytest, numpy as np
from fastapi.testclient import TestClient
from api.index import create_app

client = TestClient(create_app())

def synthetic_monotone_up():
    return np.linspace(100, 110, 60)

def synthetic_flat():
    return np.ones(60) * 100

def test_trend_monotone_up():
    close = synthetic_monotone_up()
    # Mock load_bars by monkeypatching core.bars.load_bars
    import core.bars as bars
    import pandas as pd
    original = bars.load_bars
    def mock(symbol, interval, range):
        return pd.DataFrame({"c": close, "h": close + 1, "l": close - 1})
    bars.load_bars = mock
    resp = client.get("/api/trend?symbol=RELIANCE&interval=1d&range=1Y")
    bars.load_bars = original
    assert resp.status_code == 200
    data = resp.json()
    assert data["label"] == "Uptrend"
    assert isinstance(data["strength"], float)
    assert data["ema20"] > 0 and data["ema50"] > 0
    assert data["ema20"] != data["ema50"]
    assert data["adx"] != 28.0
    assert data.get("symbol") == "RELIANCE"
    assert data.get("interval") == "1d"
    assert data.get("range") == "1Y"

def test_trend_flat():
    close = synthetic_flat()
    import core.bars as bars
    import pandas as pd
    original = bars.load_bars
    def mock(symbol, interval, range):
        return pd.DataFrame({"c": close, "h": close + 0.1, "l": close - 0.1})
    bars.load_bars = mock
    resp = client.get("/api/trend?symbol=RELIANCE&interval=1d&range=1Y")
    bars.load_bars = original
    assert resp.status_code == 200
    data = resp.json()
    assert data["label"] == "Sideways"
    assert isinstance(data["strength"], float)

def test_trend_unknown_symbol():
    import core.bars as bars
    original = bars.load_bars
    bars.load_bars = lambda s, i, r: None
    resp = client.get("/api/trend?symbol=BAD&interval=1d&range=1Y")
    bars.load_bars = original
    assert resp.status_code == 404
    data = resp.json()
    assert data.get("error", {}).get("code") == "unknownSymbol"
