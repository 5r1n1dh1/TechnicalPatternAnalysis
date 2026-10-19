"""Indicators golden-test — M4."""
import pandas as pd, numpy as np, json
from server.core import indicators

def test_rsi_macd_adx_bollinger_30bar():
    df = pd.read_csv("tests/fixtures/golden/bars30.csv")
    close = df["close"].to_numpy()
    high = df["high"].to_numpy()
    low = df["low"].to_numpy()
    r = indicators.rsi(close)
    assert not np.isnan(r[-1]), "RSI final NaN"
    line, sig, hist = indicators.macd(close)
    assert abs(hist[-1] - 0.27) < 0.5, f"MACD hist {hist[-1]}"
    adx, plus_di, minus_di = indicators.adx_di(high, low, close)
    assert adx > 20, f"ADX {adx}"
    bou, bol, w, pos, squeeze = indicators.bollinger_band(close)
    assert w > 0, "Bollinger width"
    golden = json.load(open("tests/fixtures/golden/golden30.json"))
    assert abs(r[-1] - golden["rsi14"]) < 5, f"RSI diff {abs(r[-1]-golden['rsi14'])}"
