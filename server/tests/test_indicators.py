"""Indicators golden-test — M4."""
import pandas as pd, numpy as np, json
from core import indicators

def test_rsi_macd_adx_bollinger_30bar():
    df = pd.read_csv("tests/fixtures/golden/bars30.csv")
    close = df["close"].to_numpy()
    high = df["high"].to_numpy()
    low = df["low"].to_numpy()
    r = indicators.rsi(close)
    assert not np.isnan(r[-1]), "RSI final NaN"
    line, sig, hist = indicators.macd(close)
    golden = json.load(open("tests/fixtures/golden/golden30.json"))
    assert abs(hist[-1] - golden.get("macdHistNorm", 0.27)) < 0.05, f"MACD hist {hist[-1]} vs golden {golden.get('macdHistNorm')}"  # TODO: T-06 regenerate goldens after pattern/indicator alignment
    adx, plus_di, minus_di = indicators.adx_di(high, low, close)
    assert adx > 20, f"ADX {adx}"
    bou, bol, w, pos, squeeze = indicators.bollinger_band(close)
    assert w > 0, "Bollinger width"
    golden = json.load(open("tests/fixtures/golden/golden30.json"))
    assert abs(r[-1] - golden["rsi14"]) < 5, f"RSI diff {abs(r[-1]-golden['rsi14'])}"
