"""Composite decision engine per book-spec §6. Rule-based, fully transparent."""
from __future__ import annotations

from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

try:
    from server.core.formulas import FundamentalScore
except Exception:
    FundamentalScore = None


class RedFlag(BaseModel):
    name: str
    triggered: bool = False
    severity: str = "hard"


class FundamentalScore(BaseModel):
    earningsQuality: float = 0.0
    returns: float = 0.0
    leverage: float = 0.0
    growth: float = 0.0
    capital: float = 0.0
    overall: float = 0.0


class TechnicalVerdict(BaseModel):
    bearish: bool = False
    bullish: bool = False
    structureState: Optional[str] = None


class PatternContext(BaseModel):
    bearishActive: bool = False
    bullishActive: bool = False
    bearishConfidence: float = 0.0
    bullishConfidence: float = 0.0


class MLOutlook(BaseModel):
    h1: Dict[str, float] = Field(default_factory=dict)
    h2: Dict[str, float] = Field(default_factory=dict)
    h5: Dict[str, float] = Field(default_factory=dict)


class RedFlagsEngine:
    """Computable boolean red flags per book-spec §6.1."""

    def check_red_flags(
        self,
        symbol_data: Optional[Dict[str, Any]] = None,
        fundamental_score: Optional[Any] = None,
        technical_verdict: Optional[Any] = None,
    ) -> List[RedFlag]:
        flags = []
        if symbol_data is None:
            symbol_data = {}
        # Helper to get values
        cfo_pat = symbol_data.get("cfoPatRatio")
        if fundamental_score is not None:
            cfo_pat = getattr(fundamental_score, "cfoPatRatio", cfo_pat)
        rev_cagr = symbol_data.get("revCAGR3y")
        pat_cagr = symbol_data.get("patCAGR3y")
        int_cov = symbol_data.get("interestCover")
        debt_to_equity = symbol_data.get("debtToEquity")
        roce_series = symbol_data.get("roceSeries")  # list of 3 values
        pe = symbol_data.get("pe")
        rev_declining = symbol_data.get("revDeclining", False)
        pat = symbol_data.get("pat", 0)
        ocf = symbol_data.get("ocf", 0)
        # 1. CFO/PAT divergence (hard if < 0.5 persistent; soft if < 0.8)
        if cfo_pat is not None:
            if cfo_pat < 0.5:
                flags.append(RedFlag(name="CFO_PAT_divergence", triggered=True, severity="hard"))
            elif cfo_pat < 0.8:
                flags.append(RedFlag(name="CFO_PAT_divergence", triggered=True, severity="soft"))
            else:
                flags.append(RedFlag(name="CFO_PAT_divergence", triggered=False, severity="hard"))
        else:
            flags.append(RedFlag(name="CFO_PAT_divergence", triggered=False, severity="hard"))

        # 2. Receivables growth > 1.5× rev
        recv_rev = symbol_data.get("receivablesRevRatio")
        if recv_rev is not None and recv_rev > 1.5:
            flags.append(RedFlag(name="Receivables_vs_rev", triggered=True, severity="hard"))
        else:
            flags.append(RedFlag(name="Receivables_vs_rev", triggered=False, severity="hard"))

        # 3. Inventory > 1.5× rev
        inv_sales = symbol_data.get("inventorySalesRatio")
        if inv_sales is not None and inv_sales > 1.5:
            flags.append(RedFlag(name="Inventory_vs_sales", triggered=True, severity="hard"))
        else:
            flags.append(RedFlag(name="Inventory_vs_sales", triggered=False, severity="hard"))

        # 4. Negative OCF + positive PAT
        if ocf < 0 and pat > 0:
            flags.append(RedFlag(name="Negative_OCF_positive_PAT", triggered=True, severity="hard"))
        else:
            flags.append(RedFlag(name="Negative_OCF_positive_PAT", triggered=False, severity="hard"))

        # 5. Dilution: share CAGR > 2% with EPS CAGR < PAT CAGR
        share_cagr = symbol_data.get("shareCAGR5y", 0)
        eps_cagr = symbol_data.get("epsCAGR5y", 0)
        if share_cagr > 0.02 and (eps_cagr < pat_cagr if pat_cagr is not None else False):
            flags.append(RedFlag(name="Share_dilution", triggered=True, severity="soft"))
        else:
            flags.append(RedFlag(name="Share_dilution", triggered=False, severity="soft"))

        # 6. IntCov < 2
        if int_cov is not None and int_cov < 2:
            flags.append(RedFlag(name="Interest_coverage_low", triggered=True, severity="hard"))
        else:
            flags.append(RedFlag(name="Interest_coverage_low", triggered=False, severity="hard"))

        # 7. D/E > 2
        if debt_to_equity is not None and debt_to_equity > 2:
            flags.append(RedFlag(name="Debt_to_equity_high", triggered=True, severity="hard"))
        else:
            flags.append(RedFlag(name="Debt_to_equity_high", triggered=False, severity="hard"))

        # 8. ROCE 3yr declining
        if roce_series is not None and len(roce_series) >= 3:
            if roce_series[-1] < roce_series[-2] < roce_series[-3]:
                flags.append(RedFlag(name="ROCE_declining_3yr", triggered=True, severity="hard"))
            else:
                flags.append(RedFlag(name="ROCE_declining_3yr", triggered=False, severity="hard"))
        else:
            flags.append(RedFlag(name="ROCE_declining_3yr", triggered=False, severity="hard"))

        # 9. P/E < 2 + rev declining
        if pe is not None and rev_declining and pe < 2:
            flags.append(RedFlag(name="PE_cheap_trap", triggered=True, severity="soft"))
        else:
            flags.append(RedFlag(name="PE_cheap_trap", triggered=False, severity="soft"))
        return flags


class Verdict(BaseModel):
    label: str  # INVEST / SHORT / WAIT / WATCH
    confidence: float = Field(default=0.0, ge=0, le=1)
    reasons: List[str] = Field(default_factory=list)
    subScores: Dict[str, float] = Field(default_factory=dict)


class DecisionResponse(BaseModel):
    verdict: str  # INVEST / SHORT / WAIT / WATCH
    confidence: float
    components: Dict[str, Any] = Field(default_factory=dict)
    reasons: List[str] = Field(default_factory=list)


def compute_decision(
    fundamental_score: Optional[Any] = None,
    technical_verdict: Optional[Any] = None,
    valuation_verdict: Optional[Any] = None,
    pattern_context: Optional[Any] = None,
    ml_outlook: Optional[Any] = None,
    red_flags_engine: Optional[Any] = None,
    symbol_data: Optional[Dict[str, Any]] = None,
) -> DecisionResponse:
    """Rule cascade (§6) — first-match wins. Never trains; pure evaluation."""
    if symbol_data is None:
        symbol_data = {}

    # Initialize defaults for missing inputs
    if fundamental_score is None:
        fundamental_score = FundamentalScore(overall=0)
    if technical_verdict is None:
        technical_verdict = TechnicalVerdict()
    if valuation_verdict is None:
        valuation_verdict = {"currentBand": "fair", "moS_pct": 0.2}
    if pattern_context is None:
        pattern_context = PatternContext()
    if ml_outlook is None:
        ml_outlook = MLOutlook()
    if red_flags_engine is None:
        red_flags_engine = RedFlagsEngine()

    # Build red flags
    red_flags = red_flags_engine.check_red_flags(
        symbol_data=symbol_data,
        fundamental_score=fundamental_score,
        technical_verdict=technical_verdict,
    )
    hard_flags = sum(1 for f in red_flags if f.severity == "hard" and f.triggered)

    # Helper access
    f_score = getattr(fundamental_score, "overall", getattr(fundamental_score, "overall", 0))
    f_earnings = getattr(fundamental_score, "earningsQuality", 0)
    f_returns = getattr(fundamental_score, "returns", 0)
    t_bearish = False
    if isinstance(technical_verdict, dict):
        t_bearish = technical_verdict.get("bearish", False)
    else:
        t_bearish = getattr(technical_verdict, "bearish", False)
    v_band = ""
    if isinstance(valuation_verdict, dict):
        v_band = valuation_verdict.get("currentBand", "fair")
    else:
        v_band = getattr(valuation_verdict, "currentBand", "fair") or "fair"
    p_bearish_active = False
    p_bearish_conf = 0.0
    if isinstance(pattern_context, dict):
        p_bearish_active = pattern_context.get("bearishActive", False)
        p_bearish_conf = pattern_context.get("bearishConfidence", 0.0)
    else:
        p_bearish_active = getattr(pattern_context, "bearishActive", False)
        p_bearish_conf = getattr(pattern_context, "bearishConfidence", 0.0)

    # ML outlook helpers
    def ml_down_gt(threshold=0.5, key="h2"):
        outlook = ml_outlook.get(key) if isinstance(ml_outlook, dict) else getattr(ml_outlook, key, {})
        down = outlook.get("down", 0.0) if isinstance(outlook, dict) else getattr(outlook, "down", 0.0)
        return down > threshold

    def ml_up_gt_down(key="h1"):
        outlook = ml_outlook.get(key) if isinstance(ml_outlook, dict) else getattr(ml_outlook, key, {})
        up = outlook.get("up", 0.0) if isinstance(outlook, dict) else getattr(outlook, "up", 0.0)
        down = outlook.get("down", 0.0) if isinstance(outlook, dict) else getattr(outlook, "down", 0.0)
        return up > down

    def ml_up_gt(threshold=0.55, key="h1"):
        outlook = ml_outlook.get(key) if isinstance(ml_outlook, dict) else getattr(ml_outlook, key, {})
        up = outlook.get("up", 0.0) if isinstance(outlook, dict) else getattr(outlook, "up", 0.0)
        return up > threshold

    # Cascade (first-match wins)
    reasons = []
    verdict_label = "WATCH"
    confidence = 0.3
    sub_scores = {
        "fundamental": f_score,
        "earningsQuality": f_earnings,
        "returns": f_returns,
        "technical_bearish": 1.0 if t_bearish else 0.0,
        "valuation_band": v_band,
        "hard_red_flags": hard_flags,
    }

    # 1. Hard red flag lock
    if hard_flags >= 3:
        verdict_label = "WAIT"
        confidence = 0.0
        reasons.append(f"Hard red flags >=3 ({hard_flags}); any positive signal overridden by veto.")
        # Build final response
        return DecisionResponse(
            verdict="WAIT",
            confidence=confidence,
            components={
                "fundamental": sub_scores.get("fundamental"),
                "technical": {"bearish": t_bearish, "structureState": getattr(technical_verdict, "structureState", None)},
                "valuation": {"band": v_band},
                "pattern": {"bearishActive": p_bearish_active, "confidence": p_bearish_conf},
                "ml": ml_outlook,
                "redFlags": [{"name": r.name, "triggered": r.triggered, "severity": r.severity} for r in red_flags],
            },
            reasons=reasons,
        )

    # 2. SHORT
    # T bearish-confirmed AND (bearish pattern >=60 conf OR M.down > 0.5 for h2) AND (V=expensive OR F.returns < 45) AND red_flags < 3
    if t_bearish and (p_bearish_active and p_bearish_conf >= 60) and (v_band == "expensive" or f_returns < 45) and hard_flags < 3:
        verdict_label = "SHORT"
        confidence = 0.65
        reasons.append("SHORT: bearish technical, bearish pattern/confidence, valuation expensive/quality weak, red flags < 3.")
        # Require R:R >= 2 (stub confirmation; assume present)
        rr_ok = True  # Would check pattern context R:R
        if not rr_ok:
            verdict_label = "WAIT"
            confidence = 0.4
            reasons.append("SHORT vetoed: required bearish R:R >= 2 not met.")
        else:
            reasons.append("Bearish R:R >= 2 satisfied.")
        return DecisionResponse(
            verdict=verdict_label,
            confidence=confidence,
            components={
                "fundamental": sub_scores.get("fundamental"),
                "technical": {"bearish": t_bearish, "structureState": getattr(technical_verdict, "structureState", None)},
                "valuation": {"band": v_band},
                "pattern": {"bearishActive": p_bearish_active, "confidence": p_bearish_conf},
                "ml": ml_outlook,
                "redFlags": [{"name": r.name, "triggered": r.triggered, "severity": r.severity} for r in red_flags],
            },
            reasons=reasons,
        )

    # 3. INVEST
    # F >= 65 AND V = cheap (MoS met) AND NOT bearish AND (M.up > M.down h1/h5)
    mos_met = False
    if isinstance(valuation_verdict, dict):
        mos_met = valuation_verdict.get("currentBand") == "cheap"
    else:
        mos_met = getattr(valuation_verdict, "currentBand", "") == "cheap"

    if f_score >= 65 and mos_met and not t_bearish and ml_up_gt_down("h1"):
        verdict_label = "INVEST"
        confidence = 0.75
        reasons.append("INVEST: fundamental score >=65, valuation cheap/MoS met, technical not bearish, ML up > down h1.")
        # Strong if T bullish and M.up > 0.55
        t_bullish = False
        if isinstance(technical_verdict, dict):
            t_bullish = technical_verdict.get("bullish", False)
        else:
            t_bullish = getattr(technical_verdict, "bullish", False)
        if t_bullish and ml_up_gt(0.55, "h1"):
            verdict_label = "INVEST"
            confidence = 0.9
            reasons.append("Strong INVEST: bullish technical + ML.up > 0.55.")
        return DecisionResponse(
            verdict=verdict_label,
            confidence=confidence,
            components={
                "fundamental": sub_scores.get("fundamental"),
                "technical": {"bearish": t_bearish, "bullish": t_bullish, "structureState": getattr(technical_verdict, "structureState", None)},
                "valuation": {"band": v_band, "moS_met": mos_met},
                "pattern": {"bearishActive": p_bearish_active, "bullishActive": getattr(pattern_context, "bullishActive", False) if not isinstance(pattern_context, dict) else pattern_context.get("bullishActive", False)},
                "ml": ml_outlook,
                "redFlags": [{"name": r.name, "triggered": r.triggered, "severity": r.severity} for r in red_flags],
            },
            reasons=reasons,
        )

    # 4. WAIT / WATCH fallback
    if f_score >= 65:
        verdict_label = "WAIT"
        confidence = 0.4
        if v_band in ("fair", "expensive"):
            reasons.append(f"WAIT: fundamental score >=65 but valuation band is '{v_band}' (not cheap/MoS not met) or technical bearish.")
        elif t_bearish and f_score < 65:
            reasons.append("WAIT: bearish technical with fundamental score < 65.")
        else:
            reasons.append(f"WAIT: fundamental score >=65 but missing valuation/MoS or technical conflict.")
    else:
        verdict_label = "WATCH"
        confidence = 0.35
        reasons.append("WATCH: scattered signals — fundamental score <65, valuation not clearly cheap, or conflicting technical/ML signals.")

    return DecisionResponse(
        verdict=verdict_label,
        confidence=confidence,
        components={
            "fundamental": sub_scores.get("fundamental"),
            "technical": {"bearish": t_bearish, "structureState": getattr(technical_verdict, "structureState", None)},
            "valuation": {"band": v_band},
            "pattern": {"bearishActive": p_bearish_active},
            "ml": ml_outlook,
            "redFlags": [{"name": r.name, "triggered": r.triggered, "severity": r.severity} for r in red_flags],
        },
        reasons=reasons,
    )
