"""Pure ratio formulas per book-spec §2 / C22. Transparent computation; never fabricate."""
from __future__ import annotations

from typing import Optional, Dict, Any, List
import math

try:
    from pydantic import BaseModel, Field
except Exception:
    BaseModel = object
    Field = lambda **kw: None  # type: ignore


class FundamentalScore(BaseModel):
    earningsQuality: float = Field(default=0.0, ge=0, le=100)
    returns: float = Field(default=0.0, ge=0, le=100)
    leverage: float = Field(default=0.0, ge=0, le=100)
    growth: float = Field(default=0.0, ge=0, le=100)
    capital: float = Field(default=0.0, ge=0, le=100)
    overall: float = Field(default=0.0, ge=0, le=100)
    # Sub-components for transparency
    cfoPatRatio: Optional[float] = None
    receivablesRevRatio: Optional[float] = None
    inventorySalesRatio: Optional[float] = None
    dilutionTag: Optional[str] = None
    roce: Optional[float] = None
    roic: Optional[float] = None
    roe: Optional[float] = None
    roicWaccSpread: Optional[float] = None
    debtToEquity: Optional[float] = None
    debtToEbitda: Optional[float] = None
    interestCover: Optional[float] = None
    revCAGR3y: Optional[float] = None
    patCAGR3y: Optional[float] = None
    sgr: Optional[float] = None
    fcfPositive: Optional[bool] = None
    payoutRatio: Optional[float] = None
    qualityTag: Optional[str] = None


def _mean(vals: List[float]) -> float:
    return sum(vals) / len(vals) if vals else 0.0


def compute_cagr(start: float, end: float, years: int) -> Optional[float]:
    if start is None or end is None or years < 1 or start <= 0 or end <= 0:
        return None
    return (end / start) ** (1.0 / years) - 1.0


def compute_ratios(
    income_stmt: Optional[Any] = None,
    balance_sheet: Optional[Any] = None,
    cashflow: Optional[Any] = None,
    price: Optional[float] = None,
    shares: Optional[int] = None,
    diluted_shares: Optional[int] = None,
    symbol_data: Optional[Dict[str, Any]] = None,
) -> Optional[FundamentalScore]:
    """Compute ratios; return None with tag 'insufficient_history' when impossible."""
    # Minimal input extraction: accept dicts or objects with .get/.rev etc.
    def _get(obj, key, default=None):
        if obj is None:
            return default
        if isinstance(obj, dict):
            return obj.get(key, default)
        return getattr(obj, key, default)

    # Try to read required series; if missing entirely -> insufficient
    rev = _get(income_stmt, "rev") or _get(income_stmt, "revenue")
    pat = _get(income_stmt, "pat") or _get(income_stmt, "net_income")
    ebit = _get(income_stmt, "ebit") or _get(income_stmt, "operating_income")
    cfo = _get(cashflow, "ocf") or _get(cashflow, "cash_from_operations")
    capex = _get(cashflow, "capex") or _get(cashflow, "capital_expenditures")
    ca = _get(balance_sheet, "ca") or _get(balance_sheet, "current_assets")
    cl = _get(balance_sheet, "cl") or _get(balance_sheet, "current_liabilities")
    inv = _get(balance_sheet, "inv") or _get(balance_sheet, "inventory")
    recv = _get(balance_sheet, "recv") or _get(balance_sheet, "receivables")
    debt = _get(balance_sheet, "debt")
    cash = _get(balance_sheet, "cash")
    equity = _get(balance_sheet, "equity")
    shares_now = shares or diluted_shares or _get(balance_sheet, "shares") or _get(income_stmt, "dilutedShares")
    # If no statement objects pass at all, return None
    if (rev is None or pat is None) and (income_stmt is None):
        return None  # insufficient_history implicitly
    # Build result; compute pure formulas
    score = FundamentalScore()
    # Liquidity (only if CA/CL present)
    if ca is not None and cl is not None and cl != 0:
        current = ca / cl
        # Not stored in score directly; used for quality inference
    # CFO/PAT quality
    if cfo is not None and pat is not None and pat != 0:
        score.cfoPatRatio = cfo / pat
    # Receivables vs rev
    if recv is not None and rev is not None and rev != 0:
        score.receivablesRevRatio = recv / rev
    # Inventory vs sales (approx rev)
    if inv is not None and rev is not None and rev != 0:
        score.inventorySalesRatio = inv / rev
    # Returns
    if ebit is not None and equity is not None and debt is not None:
        capital_employed = equity + (debt or 0)
        if capital_employed != 0:
            score.roce = ebit / capital_employed
    if pat is not None and equity is not None and equity != 0:
        score.roe = pat / equity
    # ROIC approx: EBIT*(1-T) / (Equity + interest-bearing debt - cash)
    t = 0.25
    if symbol_data and isinstance(symbol_data.get("taxRate"), (int, float)):
        t = symbol_data["taxRate"]
    interest_bearing_debt = debt or 0
    invested_cap = equity + interest_bearing_debt - (cash or 0)
    if invested_cap > 0 and ebit is not None:
        score.roic = (ebit * (1 - t)) / invested_cap
    # WACC spread (simplified)
    from server.core.constants import TAX_RATE, G_SEC_10Y, ERP
    # Simplified WACC = Rf + beta*ERP (beta fallback 1.0)
    beta = 1.0
    if symbol_data and isinstance(symbol_data.get("beta"), (int, float)):
        beta = symbol_data["beta"]
    wacc = G_SEC_10Y + beta * ERP
    if score.roic is not None:
        score.roicWaccSpread = score.roic - wacc
    # Leverage
    if debt is not None and equity is not None and equity != 0:
        score.debtToEquity = debt / equity
    if ebit is not None and (debt is not None) and debt != 0:
        # Debt/EBITDA: need EBITDA
        ebitda = _get(income_stmt, "ebitda")
        if ebitda is None and ebit is not None:
            d_a = _get(income_stmt, "d_a") or 0
            ebitda = ebit + (d_a or 0)
        if ebitda and ebitda != 0:
            score.debtToEbitda = debt / ebitda
    # Interest cover
    int_exp = _get(income_stmt, "int_exp") or _get(income_stmt, "interest_expense")
    if int_exp is not None and int_exp != 0:
        score.interestCover = ebit / int_exp if ebit is not None else None
    # Growth (CAGR 3-yr) — requires 3 previous years history; use symbol_data
    rev_3y = symbol_data.get("rev_3y") if symbol_data else None
    rev_now = rev
    if rev_3y is not None and rev_now is not None and rev_3y > 0 and rev_now > 0:
        score.revCAGR3y = compute_cagr(rev_3y, rev_now, 3)
    pat_3y = symbol_data.get("pat_3y") if symbol_data else None
    if pat_3y is not None and pat is not None and pat_3y > 0 and pat > 0:
        score.patCAGR3y = compute_cagr(pat_3y, pat, 3)
    # SGR = ROE * retention; retention = 1 - payout
    payout = None
    if symbol_data and isinstance(symbol_data.get("payout"), (int, float)):
        payout = symbol_data["payout"]
    if score.roe is not None and payout is not None:
        retention = 1 - payout
        score.sgr = score.roe * retention
    # Capital
    if cfo is not None and (capex is not None):
        score.fcfPositive = (cfo - capex) > 0
    if payout is not None:
        score.payoutRatio = payout
    # Overall score approximated
    # Earnings quality (0-100) mapped roughly
    eq_score = 50.0
    if score.cfoPatRatio is not None:
        if score.cfoPatRatio >= 0.8:
            eq_score = 85 + (min(score.cfoPatRatio, 2.0) - 0.8) * 25
        elif score.cfoPatRatio < 0.5:
            eq_score = max(0, score.cfoPatRatio * 60)
        else:
            eq_score = 50 + (score.cfoPatRatio - 0.5) * 100 / 0.3
    if score.receivablesRevRatio is not None and score.receivablesRevRatio > 1.5:
        eq_score -= 20
    if score.inventorySalesRatio is not None and score.inventorySalesRatio > 1.5:
        eq_score -= 10
    score.earningsQuality = max(0, min(100, eq_score))
    # Returns score
    r_score = 50.0
    if score.roce is not None:
        if score.roce > 20:
            r_score = 90 + min((score.roce - 20) / 10, 1.0) * 10
        elif score.roce > 15:
            r_score = 75 + (score.roce - 15) * 3
        elif score.roce > 10:
            r_score = 50 + (score.roce - 10) * 5
        else:
            r_score = max(0, score.roce * 5)
    score.returns = max(0, min(100, r_score))
    # Leverage score
    l_score = 50.0
    if score.debtToEquity is not None:
        if score.debtToEquity < 0.3:
            l_score = 80
        elif score.debtToEquity < 1.0:
            l_score = 65
        elif score.debtToEquity < 2.0:
            l_score = 40
        else:
            l_score = max(0, 40 - (score.debtToEquity - 2) * 20)
    if score.interestCover is not None and score.interestCover < 2:
        l_score -= 20
    score.leverage = max(0, min(100, l_score))
    # Growth score
    g_score = 50.0
    rev_cagr = score.revCAGR3y or 0
    if rev_cagr is not None:
        g_score = min(100, max(0, rev_cagr * 10))
    else:
        g_score = 30  # no history
    score.growth = max(0, min(100, g_score))
    # Capital score
    c_score = 50.0
    if score.fcfPositive:
        c_score = 70
    else:
        c_score = 30
    if score.payoutRatio is not None:
        if score.payoutRatio < 0.7:
            c_score += 15
        else:
            c_score -= 10
    score.capital = max(0, min(100, c_score))
    # Overall composite (weighted roughly per spec)
    score.overall = (
        0.25 * score.earningsQuality +
        0.25 * score.returns +
        0.20 * score.leverage +
        0.15 * score.growth +
        0.15 * score.capital
    )
    # Quality tier tag
    tier = "tier3"
    if score.roce is not None and score.cfoPatRatio is not None:
        if score.roce >= 20 and score.cfoPatRatio >= 0.8:
            tier = "tier1"
        elif score.roce >= 10 and score.cfoPatRatio >= 0.5:
            tier = "tier2"
        elif rev is not None and pat is not None and pat < 0:
            tier = "cyclical"  # negative earnings -> treat cautiously
    score.qualityTag = tier
    # If key inputs missing and no ratios computed -> tag insufficient_history implicitly via tag
    # Return always the score object (never None when some input present); only None if totally impossible
    return score
