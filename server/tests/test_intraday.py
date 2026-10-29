"""Unit tests for the keyless intraday providers (no network)."""
import pandas as pd

import core.intraday as intraday


class _Response:
    def __init__(self, payload):
        self._payload = payload

    def raise_for_status(self):
        return None

    def json(self):
        return self._payload


def _epoch(value: str) -> int:
    return int(pd.Timestamp(value, tz="Asia/Kolkata").timestamp())


def test_groww_volume_is_deaccumulated_per_session(monkeypatch):
    candles = [
        [_epoch("2026-10-28 09:15"), 100, 101, 99, 100.5, 100],
        [_epoch("2026-10-28 09:20"), 100.5, 102, 100, 101, 250],
        [_epoch("2026-10-28 09:25"), 101, 101.5, 100.5, 101.2, None],
        [_epoch("2026-10-28 09:30"), 101.2, 102.5, 101, 102, 400],
        [_epoch("2026-10-29 09:15"), 102, 103, 101.5, 102.5, 50],
        [_epoch("2026-10-29 09:20"), 102.5, 103.5, 102, 103, 120],
    ]
    import httpx
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _Response({"candles": candles}))

    df = intraday._from_groww("RELIANCE", "5m", "5D")

    assert list(df["v"]) == [100.0, 150.0, 0.0, 150.0, 50.0, 70.0]
    assert len(df) == 6


def test_yahoo_skips_bars_without_ohlc(monkeypatch):
    payload = {
        "chart": {
            "result": [
                {
                    "timestamp": [1, 2, 3],
                    "indicators": {
                        "quote": [
                            {
                                "open": [10.0, None, 12.0],
                                "high": [11.0, None, 13.0],
                                "low": [9.5, None, 11.5],
                                "close": [10.5, None, 12.5],
                                "volume": [100, 0, 300],
                            }
                        ]
                    },
                }
            ],
            "error": None,
        }
    }
    import httpx
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _Response(payload))

    df = intraday._from_yahoo("RELIANCE", "5m", "5D")

    assert len(df) == 2
    assert list(df["c"]) == [10.5, 12.5]
    assert list(df["v"]) == [100.0, 300.0]
