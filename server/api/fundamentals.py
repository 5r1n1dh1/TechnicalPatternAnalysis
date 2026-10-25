"""Fundamentals endpoint — real yfinance data, nulls when unavailable."""
import time
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional

from core.formulas import compute_ratios

router = APIRouter()

# Module-level 12h TTL cache (no per-request disk thrash)
_fund_cache: dict = {}
_CACHE_TTL = 12 * 3600


class FundamentalsHeader(BaseModel):
    name: str
    sector: str
    mcap: float
    fiftyTwoWRange: str
    beta: float


class FundamentalsValuation(BaseModel):
    pe_ttm: Optional[float]
    pe_forward: Optional[float]
    pb: Optional[float]
    dividendYield: Optional[float]
    eps: Optional[float]
    bookValue: Optional[float]


class FundamentalsProfitability(BaseModel):
    roa: Optional[float]
    roce: Optional[float]
    roic: Optional[float]
    grossMargin: Optional[float]
    netMargin: Optional[float]


class QuarterlyResult(BaseModel):
    q: str
    rev: float
    pat: float
    eps: float
    yoyRev: Optional[float]


class AnnualPNL(BaseModel):
    revenue: Optional[float]
    pat: Optional[float]


class AnnualBalanceSheet(BaseModel):
    totalAssets: Optional[float]
    totalDebt: Optional[float]
    equity: Optional[float]


class AnnualCashFlow(BaseModel):
    ocf: Optional[float]
    capex: Optional[float]
    fcf: Optional[float]


class AnnualStatements(BaseModel):
    pnl: AnnualPNL
    balanceSheet: AnnualBalanceSheet
    cashFlow: AnnualCashFlow


class RedFlags(BaseModel):
    earningsQuality: bool
    leverage: bool
    growth: bool
    management: bool


class Shareholding(BaseModel):
    promoter: Optional[float]
    fii: Optional[float]
    dii: Optional[float]
    public: Optional[float]


class FundamentalsResponse(BaseModel):
    symbol: str
    asOf: Optional[str]
    header: FundamentalsHeader
    valuation: FundamentalsValuation
    profitability: FundamentalsProfitability
    quarterlyResults: List[QuarterlyResult]
    annualStatements: AnnualStatements
    redFlags: RedFlags
    shareholding: Shareholding
    provenance: dict


def _f(value, scale: float = 1.0) -> Optional[float]:
    """Return float(value)*scale or None when unavailable/NaN."""
    try:
        if value is None:
            return None
        out = float(value) * scale
        if out != out or out in (float("inf"), float("-inf")):
            return None
        return out
    except (TypeError, ValueError):
        return None


def _ns_symbol(symbol: str) -> str:
    symbol = (symbol or "").strip().upper()
    if not symbol:
        return ""
    if symbol.endswith((".NS", ".BO")):
        return symbol
    return symbol + ".NS"


def _quarterly_rows(ticker) -> List[QuarterlyResult]:
    rows: List[QuarterlyResult] = []
    try:
        q = ticker.quarterly_income_stmt
        if q is None or q.empty:
            return rows
        revenue = q.loc["Total Revenue"] if "Total Revenue" in q.index else None
        net_income = q.loc["Net Income"] if "Net Income" in q.index else None
        eps = q.loc["Diluted EPS"] if "Diluted EPS" in q.index else None
        for i, col in enumerate(q.columns):
            label = str(col)
            rev = _f(revenue.iloc[i] if revenue is not None else None)
            pat = _f(net_income.iloc[i] if net_income is not None else None)
            ep = _f(eps.iloc[i] if eps is not None else None)
            if rev is None and pat is None:
                continue
            yoy = None
            if i + 4 < len(q.columns):
                prev_rev = _f(revenue.iloc[i + 4] if revenue is not None else None)
                if rev and prev_rev:
                    yoy = (rev - prev_rev) / abs(prev_rev)
            rows.append(QuarterlyResult(q=label, rev=rev or 0.0, pat=pat or 0.0, eps=ep or 0.0, yoyRev=yoy))
    except Exception:
        return rows
    return rows


@router.get("/fundamentals", response_model=FundamentalsResponse)
def get_fundamentals(symbol: Optional[str] = None):
    """Get financial statements and ratios from yfinance (EOD/fundamental data)."""
    symbol = (symbol or "RELIANCE").strip().upper()
    # Cache guard: module-level TTL, no per-request disk thrash
    now = time.time()
    cached = _fund_cache.get(symbol)
    if cached is not None:
        payload, ts = cached
        if now - ts < _CACHE_TTL:
            return payload
    # If stale/missing upstream → 503; unknown symbol → 404
    as_of = None
    try:
        import yfinance as yf
        from datetime import datetime, timezone
        as_of = datetime.now(timezone.utc).isoformat()
        ticker = yf.Ticker(_ns_symbol(symbol))
        info = ticker.info or {}
    except Exception:
        info = {}
        ticker = None
    if ticker is None or not info:
        if ticker is None:
            raise HTTPException(status_code=503, detail="upstreamUnavailable")
        # ticker exists but info empty → unknown symbol
        raise HTTPException(status_code=404, detail="unknown symbol")
    # Unknown symbol when no identifying info fields (fallback guard)
    if not info.get("longName") and not info.get("shortName") and not info.get("symbol"):
        raise HTTPException(status_code=404, detail="unknown symbol")

    # Real data load continues
    price = _f(info.get("currentPrice") or info.get("regularMarketPrice"))
    mcap = _f(info.get("marketCap"))
    beta = _f(info.get("beta"))
    fifty_two_low = _f(info.get("fiftyTwoWeekLow"))
    fifty_two_high = _f(info.get("fiftyTwoWeekHigh"))
    fifty_two = f"{fifty_two_low}-{fifty_two_high}" if fifty_two_low is not None and fifty_two_high is not None else "N/A"

    revenue = _f(info.get("totalRevenue"))
    pat = _f(info.get("netIncomeToCommon"))
    ebitda = _f(info.get("ebitda"))
    total_assets = _f(info.get("totalAssets"))
    total_debt = _f(info.get("totalDebt"))
    cash = _f(info.get("totalCash"))
    equity = None
    if total_assets is not None and total_debt is not None:
        equity = total_assets - total_debt
    cfo = _f(info.get("operatingCashflow"))
    capex = _f(info.get("capitalExpenditures"))
    capex_abs = abs(capex) if capex is not None else None
    fcf = (cfo - capex_abs) if (cfo is not None and capex_abs is not None) else None

    quarterly = _quarterly_rows(ticker) if ticker is not None else []

    cfo_pat = (cfo / pat) if (cfo is not None and pat not in (None, 0)) else None
    debt_to_equity = (total_debt / equity) if (total_debt is not None and equity not in (None, 0)) else None
    rev_growth = info.get("revenueGrowth")
    if isinstance(rev_growth, dict):
        rev_growth_pct = _f(rev_growth.get("percent"), 0.01)
    elif isinstance(rev_growth, (int, float)):
        rev_growth_pct = _f(rev_growth)
    else:
        rev_growth_pct = None

    # ROCE: prefer info value; else compute via core.formulas from statements
    roce_raw = info.get("returnOnCapitalEmployed")
    roce = _f(roce_raw, 100.0) if roce_raw is not None else None
    equity_info = info.get("stockholdersEquity") or info.get("totalEquity")
    eq_for_score = equity_info or equity
    if roce is None and eq_for_score is not None:
        score = compute_ratios(
            income_stmt={"ebit": info.get("ebit"), "rev": info.get("totalRevenue"), "pat": info.get("netIncomeToCommon")},
            balance_sheet={"equity": eq_for_score, "debt": info.get("totalDebt"), "cash": info.get("totalCash")},
            cashflow={"ocf": info.get("operatingCashflow"), "capex": info.get("capitalExpenditures")},
            symbol_data={"beta": info.get("beta")},
        )
        roce = score.roce if score else None

    red_flags = RedFlags(
        earningsQuality=bool(cfo_pat is not None and cfo_pat < 0.8),
        leverage=bool(debt_to_equity is not None and debt_to_equity > 2),
        growth=bool(rev_growth_pct is not None and rev_growth_pct < 0),
        management=False,
    )

    result = FundamentalsResponse(
        symbol=symbol,
        asOf=as_of,
        header=FundamentalsHeader(
            name=str(info.get("longName") or info.get("shortName") or symbol),
            sector=str(info.get("sector") or "N/A"),
            mcap=mcap or 0.0,
            fiftyTwoWRange=fifty_two,
            beta=beta or 0.0,
        ),
        valuation=FundamentalsValuation(
            pe_ttm=_f(info.get("trailingPE")),
            pe_forward=_f(info.get("forwardPE")),
            pb=_f(info.get("priceToBook")),
            dividendYield=_f(info.get("dividendYield"), 100.0),
            eps=_f(info.get("trailingEps")),
            bookValue=_f(info.get("bookValue")),
        ),
        profitability=FundamentalsProfitability(
            roa=_f(info.get("returnOnAssets"), 100.0),
            roce=roce,
            roic=_f(info.get("returnOnInvestedCapital"), 100.0),
            grossMargin=_f(info.get("grossMargins"), 100.0),
            netMargin=_f(info.get("profitMargins"), 100.0),
        ),
        quarterlyResults=quarterly,
        annualStatements=AnnualStatements(
            pnl=AnnualPNL(revenue=revenue, pat=pat),
            balanceSheet=AnnualBalanceSheet(totalAssets=total_assets, totalDebt=total_debt, equity=equity),
            cashFlow=AnnualCashFlow(ocf=cfo, capex=capex_abs, fcf=fcf),
        ),
        redFlags=red_flags,
        shareholding=Shareholding(
            promoter=_f(info.get("heldPercentInsiders"), 100.0),
            fii=_f(info.get("heldPercentInstitutions"), 100.0),
            dii=None,
            public=_f(info.get("heldPercentInstitutions"), 100.0),
        ),
        provenance={
            "source": "yfinance",
            "fields": "annual:info",
            "symbol": _ns_symbol(symbol),
        },
    )
    _fund_cache[symbol] = (result, now)
    return result
