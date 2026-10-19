"""Fundamentals endpoint."""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional

router = APIRouter()


class FundamentalsHeader(BaseModel):
    name: str
    sector: str
    mcap: float
    fiftyTwoWRange: str
    beta: float


class FundamentalsValuation(BaseModel):
    pe_ttm: float
    pe_forward: float
    pb: float
    dividendYield: float
    eps: float
    bookValue: float


class FundamentalsProfitability(BaseModel):
    roa: float
    roce: float
    roic: float
    grossMargin: float
    netMargin: float


class QuarterlyResult(BaseModel):
    q: str
    rev: float
    pat: float
    eps: float
    yoyRev: float


class AnnualPNL(BaseModel):
    revenue: float
    pat: float


class AnnualBalanceSheet(BaseModel):
    totalAssets: float
    totalDebt: float
    equity: float


class AnnualCashFlow(BaseModel):
    ocf: float
    capex: float
    fcf: float


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
    promoter: float
    fii: float
    dii: float
    public: float


class FundamentalsResponse(BaseModel):
    header: FundamentalsHeader
    valuation: FundamentalsValuation
    profitability: FundamentalsProfitability
    quarterlyResults: List[QuarterlyResult]
    annualStatements: AnnualStatements
    redFlags: RedFlags
    shareholding: Shareholding


@router.get("/fundamentals", response_model=FundamentalsResponse)
def get_fundamentals(symbol: Optional[str] = None):
    """Get financial statements and ratios."""
    # Return minimal valid response
    return FundamentalsResponse(
        header=FundamentalsHeader(
            name="N/A", sector="N/A", mcap=0.0, fiftyTwoWRange="0-0", beta=0.0
        ),
        valuation=FundamentalsValuation(
            pe_ttm=0.0, pe_forward=0.0, pb=0.0, dividendYield=0.0, eps=0.0, bookValue=0.0
        ),
        profitability=FundamentalsProfitability(
            roa=0.0, roce=0.0, roic=0.0, grossMargin=0.0, netMargin=0.0
        ),
        quarterlyResults=[],
        annualStatements=AnnualStatements(
            pnl=AnnualPNL(revenue=0.0, pat=0.0),
            balanceSheet=AnnualBalanceSheet(totalAssets=0.0, totalDebt=0.0, equity=0.0),
            cashFlow=AnnualCashFlow(ocf=0.0, capex=0.0, fcf=0.0),
        ),
        redFlags=RedFlags(earningsQuality=False, leverage=False, growth=False, management=False),
        shareholding=Shareholding(promoter=0.0, fii=0.0, dii=0.0, public=0.0),
    )
