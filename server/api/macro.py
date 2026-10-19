"""Macro endpoint — real EOD indicators; null when unavailable (no hard-coded values)."""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()


class MacroResponse(BaseModel):
    asOf: str
    vix: Optional[float]
    usdinr: Optional[float]
    crude: Optional[float]
    fiiDiiNet: Optional[float]
    gsec10y: Optional[float]
    provenance: dict


def _last_close(ticker_symbol: str) -> Optional[float]:
    """Last EOD close for a yfinance symbol, or None when unavailable."""
    try:
        import yfinance as yf
        hist = yf.Ticker(ticker_symbol).history(period="5d")
        if hist is None or hist.empty or "Close" not in hist.columns:
            return None
        value = float(hist["Close"].iloc[-1])
        if value != value:
            return None
        return value
    except Exception:
        return None


@router.get("/macro", response_model=MacroResponse)
def get_macro():
    from datetime import datetime, timezone

    sources = {
        "vix": ("INDIAVIX.NS", _last_close("INDIAVIX.NS")),
        "usdinr": ("USDINR=X", _last_close("USDINR=X")),
        "crude": ("CL=F", _last_close("CL=F")),
    }
    provenance = {
        "source": "yfinance",
        "fields": {k: (v[0] if v[1] is not None else None) for k, v in sources.items()},
        "asOf": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S+05:30"),
    }
    return MacroResponse(
        asOf=provenance["asOf"],
        vix=sources["vix"][1],
        usdinr=sources["usdinr"][1],
        crude=sources["crude"][1],
        fiiDiiNet=None,
        gsec10y=None,
        provenance=provenance,
    )
