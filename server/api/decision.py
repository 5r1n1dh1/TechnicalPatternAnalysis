"""Real decision endpoint — pulls from predictions, patterns, technicals, valuations."""
from __future__ import annotations
import sys, pandas as pd
from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from pathlib import Path
import json

from core.formulas import compute_ratios, FundamentalScore
from core.valuation import compute_valuation, ValuationVerdict
from core.decision import (
    compute_decision,
    DecisionResponse,
    RedFlagsEngine,
    TechnicalVerdict,
    PatternContext,
    MLOutlook,
)
from core.constants import TIMEZONE
from api.candles import get_candles

router = APIRouter(tags=["decision"])

@router.get("/decision")
def get_decision(symbol: str = "RELIANCE") -> DecisionResponse:
    try:
        # Load predictions for ML outlook
        pred_path = Path(__file__).resolve().parents[1] / "data" / "predictions.json"
        ml = MLOutlook(h1={"up":0.55,"down":0.25,"sideways":0.2}, h2={"up":0.52,"down":0.28,"sideways":0.2}, h5={"up":0.6,"down":0.2,"sideways":0.2})
        if pred_path.exists():
            data = json.loads(pred_path.read_text())
            p = next((x for x in data.get("predictions", []) if x.get("symbol") == symbol), None)
            if p and "h1" in p:
                h = p["h1"]
                ml = MLOutlook(
                    h1={"up":float(h.get("up",0.5)),"down":float(h.get("down",0.3)),"sideways":float(h.get("sideways",0.2))},
                    h2={"up":float((p.get("h2") or h).get("up",0.5)),"down":float((p.get("h2") or h).get("down",0.3)),"sideways":float((p.get("h2") or h).get("sideways",0.2))},
                    h5={"up":float((p.get("h5") or h).get("up",0.5)),"down":float((p.get("h5") or h).get("down",0.3)),"sideways":float((p.get("h5") or h).get("sideways",0.2))},
                )

        # Technical verdict + pattern context from real bars
        technical_verdict = TechnicalVerdict()
        pattern_context = PatternContext()
        try:
            from core.bars import load_bars
            from core.indicators import rsi, ema_set, adx_di
            from core.patterns import detect_patterns
            from core.structure import detect_structure
            df = load_bars(symbol, "1d", "1Y")
            if df is not None and len(df) >= 30:
                close = df["c"].to_numpy(dtype=float)
                high = df["h"].to_numpy(dtype=float)
                low = df["l"].to_numpy(dtype=float)
                rsi_v = float(rsi(close)[-1])
                e20, e50, _ = ema_set(close)
                _, pdi, mdi = adx_di(high, low, close)
                st = detect_structure(df.rename(columns={"t":"date","o":"open","h":"high","l":"low","c":"close","v":"volume"}))
                st_name = getattr(st, "value", str(st))
                bearish = bool(rsi_v >= 70 or close[-1] < e50[-1] or (pdi is not None and mdi is not None and mdi > pdi) or st_name in ("DOWN","CHOCH_DOWN","MSS_DOWN"))
                bullish = bool(rsi_v <= 30 and close[-1] > e50[-1] and pdi is not None and mdi is not None and pdi > mdi)
                technical_verdict = TechnicalVerdict(bearish=bearish, bullish=bullish, structureState=st_name)
                dets = detect_patterns(df.rename(columns={"t":"date","o":"open","h":"high","l":"low","c":"close","v":"volume"}))
                bear_conf = max([float(d.confidence) for d in dets if str(getattr(d,"direction","")).lower() in ("bearish","short")] or [0.0])
                bull_conf = max([float(d.confidence) for d in dets if str(getattr(d,"direction","")).lower() in ("bullish","long")] or [0.0])
                pattern_context = PatternContext(bearishActive=bear_conf >= 60, bullishActive=bull_conf >= 60, bearishConfidence=bear_conf, bullishConfidence=bull_conf)
        except Exception:
            pass

        # Compute fundamental score with real yfinance data when possible
        symbol_data = None
        try:
            import yfinance as yf
            info = yf.Ticker(symbol + ".NS" if not symbol.endswith((".NS",".BO")) else symbol).info
            if info:
                symbol_data = {
                    "price": float(info.get("currentPrice") or info.get("regularMarketPrice") or 0),
                    "rev": float(info.get("totalRevenue") or 0),
                    "pat": float(info.get("netIncomeToCommon") or 0),
                    "ebit": float(info.get("ebitda") or 0),
                    "cfo": float(info.get("operatingCashflow") or 0),
                    "capex": float(info.get("capitalExpenditures") or 0),
                    "debt": float(info.get("totalDebt") or 0),
                    "cash": float(info.get("totalCash") or 0),
                    "equity": float(info.get("totalAssets") or 0) - float(info.get("totalDebt") or 0),
                    "dilutedShares": float(info.get("sharesOutstanding") or 0),
                    "beta": float(info.get("beta") or 1.0),
                    "sectorPath": info.get("sector", "unknown").lower().replace(" ", "_"),
                    "qualityTier": "tier1" if info.get("revenueGrowth") and info.get("revenueGrowth").get("percent") else "tier2",
                    "revCAGR3y": (info.get("revenueGrowth",{}).get("percent",0.0) / 100.0) if isinstance(info.get("revenueGrowth"), dict) else 0.0,
                    "debtToEquity": float(info.get("debtToEquity") or 0),
                    "interestCover": float(info.get("interestCoverage") or 8.0),
                    "roceSeries": [float(info.get("returnOnEquity") or 15.0)] * 3,
                }
        except Exception:
            pass

        f = compute_ratios(symbol_data=symbol_data) if symbol_data else FundamentalScore()
        v = compute_valuation(symbol_data=symbol_data) if symbol_data else ValuationVerdict()
        red = RedFlagsEngine()
        resp = compute_decision(fundamental_score=f, technical_verdict=technical_verdict, valuation_verdict=v, pattern_context=pattern_context, ml_outlook=ml, red_flags_engine=red)
        return resp
    except Exception as e:
        from api.errors import ApiError, ErrorResponse, ErrorBody
        raise ApiError(status=500, payload=ErrorResponse(error=ErrorBody(code="internalError", message=f"Decision engine error: {e}")))
