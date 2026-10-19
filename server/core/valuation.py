"""Valuation engine per book-spec §4 / C22."""
from __future__ import annotations

from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

try:
    import numpy as np
except Exception:
    np = None  # type: ignore

from server.core.constants import (
    G_SEC_10Y, ERP, TAX_RATE,
    WACC_BULL_G, WACC_BASE_G, WACC_BEAR_G,
    MO_QUALITY_TIERS,
)


class DcfScenario(BaseModel):
    scenario: str  # bull / base / bear
    wacc: float
    terminalG: float
    fcfMarginBase: float
    equityValue: float
    intrinsicValuePerShare: float
    upsidePct: float


class ValuationVerdict(BaseModel):
    multipleName: str = "EV/EBITDA"
    currentBand: str = "fair"  # cheap / fair / expensive
    peg: Optional[float] = None
    earningsYield: Optional[float] = None
    dcfScenarios: List[DcfScenario] = Field(default_factory=list)
    moS_pct: float = 0.0
    normalizedEarnings: Optional[float] = None
    bankAnchorPb: Optional[float] = None
    intrinsicValuePerShare: float = 0.0
    upside_pct: float = 0.0
    qualityTier: str = "tier2"
    sectorPath: str = "general"


def _interpolate_pb_roe(roe_pct: float) -> float:
    # Book table: RoE 10→1.2×, 18→3.5×, 20→4.5×
    if roe_pct <= 10:
        return 1.2 + (roe_pct - 10) * 0.0  # floor at 1.2
    if roe_pct >= 20:
        return 4.5 + (roe_pct - 20) * 0.0  # cap at 4.5
    # Linear interpolation between 10 and 20
    return 1.2 + (roe_pct - 10) * (4.5 - 1.2) / 10.0


def compute_valuation(symbol_data: Optional[Dict[str, Any]] = None) -> ValuationVerdict:
    """Compute valuation verdict from symbol data dictionary (cached predictions + fundamentals)."""
    if symbol_data is None:
        symbol_data = {}
    # Defaults for stub
    price = symbol_data.get("price", 100.0)
    rev = symbol_data.get("rev") or 5000.0
    pat = symbol_data.get("pat") or 500.0
    ebit = symbol_data.get("ebit") or 1000.0
    capex = symbol_data.get("capex") or 300.0
    debt = symbol_data.get("debt") or 2000.0
    cash = symbol_data.get("cash") or 500.0
    equity = symbol_data.get("equity") or 3000.0
    diluted_shares = symbol_data.get("dilutedShares") or 100
    beta = symbol_data.get("beta", 1.0)
    sector_path = symbol_data.get("sectorPath", "general")

    # Multiple selection by sector path (§4.1)
    multiple_name = "EV/EBITDA"
    if sector_path in ("banks", "nbfc", "insurance", "bank_nbfc"):
        multiple_name = "P/B"
    elif sector_path in ("it", "fmcg", "healthcare", "pharma"):
        multiple_name = "P/E"
    elif sector_path in ("retail",):
        multiple_name = "EV/Sales"

    # Earnings yield
    eps = pat / diluted_shares if diluted_shares else 0.0
    earnings_yield = 1.0 / (price / eps) if price and eps and eps > 0 else None
    if earnings_yield is not None:
        earnings_yield = earnings_yield  # as decimal

    # PEG (if growth available)
    rev_cagr = symbol_data.get("revCAGR3y")
    peg = None
    if earnings_yield is not None and rev_cagr is not None and rev_cagr > 0:
        pe = price / eps if eps > 0 else None
        if pe:
            peg = pe / (rev_cagr * 100)  # book uses growth % (percent), not decimal
    # Adjust peg to standard decimal if needed: if > 10 likely percent error; use approximate
    if peg is not None and peg > 20:
        peg = (price / eps) / rev_cagr if rev_cagr > 0 else None

    # DCF scenarios
    cfo = symbol_data.get("cfo", 0)
    capex_val = capex if capex else 0
    fcf = cfo - capex_val
    fcf_margin = fcf / rev if rev else 0

    scenarios = []
    for label, term_g in [("bull", WACC_BULL_G), ("base", WACC_BASE_G), ("bear", WACC_BEAR_G)]:
        # WACC approximation (simplified)
        wacc = G_SEC_10Y + beta * ERP
        # Growth start at min(rev_cagr or 10%, 18%) decaying to terminal g over 10y
        growth_start = min(rev_cagr or 0.10, 0.18) if rev_cagr is not None else 0.10
        # Simple linear decay approximation: use average growth over horizon
        avg_growth = (growth_start + term_g) / 2
        # 10-year projection simplified as growing annuity approximation (not full year-by-year)
        # TV = lastFCF*(1+g)/(WACC-g)
        last_fcf = fcf * (1 + avg_growth) ** 10
        if wacc > term_g:
            tv = last_fcf * (1 + term_g) / (wacc - term_g)
        else:
            tv = last_fcf
        # Equity value = EV approximation (simplified; no detailed debt schedule)
        # Use simple sum: PV of 10y FCF + TV; approximate PV with factor 1/(1+wacc)^5 average
        # For minimal correctness: approximate EV ≈ FCF*(1+avg_g)/(wacc - avg_g) as rough perpetuity
        # Instead, use a rough 10-year DCF approximation
        years = 10
        pv = 0.0
        g = growth_start
        for y in range(1, years + 1):
            # Linear decay to terminal g
            decayed_g = growth_start - (growth_start - term_g) * (y / years)
            fcf_y = fcf * (1 + decayed_g) ** y
            pv += fcf_y / ((1 + wacc) ** y)
        if wacc > term_g:
            tv = (fcf * (1 + term_g) ** years * (1 + term_g)) / (wacc - term_g)
        else:
            tv = fcf * (1 + term_g) ** years
        pv += tv / ((1 + wacc) ** years)
        equity_value = pv - (debt - cash)
        iv_per_share = equity_value / diluted_shares if diluted_shares else 0.0
        upside = (iv_per_share - price) / price if price else 0.0
        scenarios.append(DcfScenario(
            scenario=label,
            wacc=round(wacc, 4),
            terminalG=term_g,
            fcfMarginBase=round(fcf_margin, 4),
            equityValue=round(equity_value, 2),
            intrinsicValuePerShare=round(iv_per_share, 2),
            upsidePct=round(upside, 4),
        ))

    # Margin of Safety vs quality tier
    tier = symbol_data.get("qualityTier", "tier2")
    mo_config = MO_QUALITY_TIERS.get(tier, MO_QUALITY_TIERS["tier2"])
    # Required MoS %: if cheap band and quality high -> lower MoS allowed; else use tier min
    # For simplicity, set moS_pct to tier minimum
    mo_s_pct = mo_config.get("mos_min", 0.20)

    # Bank/NBFC path
    bank_anchor_pb = None
    if sector_path in ("banks", "nbfc", "bank_nbfc"):
        roe_pct = symbol_data.get("roe")
        if roe_pct is not None:
            bank_anchor_pb = _interpolate_pb_roe(roe_pct)

    # Normalized earnings for cyclicals
    normalized_earnings = None
    if tier == "cyclical":
        # Mean of last 5-yr EPS; approximate from PAT / shares
        # If 5-year mean available in symbol_data
        mean_eps = symbol_data.get("normalizedEPS") or (pat / diluted_shares)
        normalized_earnings = mean_eps

    # Current band: based on multiple vs 5yr band approximation (stub)
    current_band = "fair"
    # If DCF upside > 0.25 and earnings yield > G_SEC -> cheap; else fair; if upside < -0.15 -> expensive
    avg_upside = sum(s.upsidePct for s in scenarios) / len(scenarios) if scenarios else 0.0
    if earnings_yield is not None and earnings_yield > G_SEC_10Y / 100:
        current_band = "cheap"
    elif avg_upside < -0.15:
        current_band = "expensive"
    else:
        current_band = "fair"

    # Pick base intrinsic / upside for summary
    base_scenario = next((s for s in scenarios if s.scenario == "base"), scenarios[1] if len(scenarios) > 1 else None)
    iv_summary = base_scenario.intrinsicValuePerShare if base_scenario else 0.0
    upside_summary = base_scenario.upsidePct if base_scenario else 0.0

    verdict = ValuationVerdict(
        multipleName=multiple_name,
        currentBand=current_band,
        peg=peg,
        earningsYield=round(earnings_yield, 4) if earnings_yield is not None else None,
        dcfScenarios=scenarios,
        moS_pct=mo_s_pct,
        normalizedEarnings=normalized_earnings,
        bankAnchorPb=bank_anchor_pb,
        intrinsicValuePerShare=iv_summary,
        upside_pct=upside_summary,
        qualityTier=tier,
        sectorPath=sector_path,
    )
    return verdict
