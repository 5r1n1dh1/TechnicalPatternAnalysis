

@Serializable
data class FundamentalsResponse(
    val header: Header = Header(),
    val valuation: Valuation = Valuation(),
    val profitability: Profitability = Profitability(),
    val ratios: List<RatioItem> = emptyList(),
    val quarterlyResults: List<QuarterlyResult> = emptyList(),
    val annualStatements: AnnualStatements = AnnualStatements(),
    val redFlags: RedFlags = RedFlags(),
    val shareholding: Shareholding = Shareholding(),
    val corporateActions: List<CorporateAction> = emptyList(),
    val dcf: DcfEstimate = DcfEstimate()
)

@Serializable
data class Header(
    val name: String = "",
    val sector: String = "",
    val mcap: Double = 0.0,
    @SerialName("w52Range") val w52Range: String = "",
    @SerialName("52wRange") val w52RangeAlt: String = w52Range,
    val beta: Double = 0.0
)

@Serializable
data class Valuation(
    val pe_ttm: Double? = null,
    val pe_forward: Double? = null,
    val pb: Double? = null,
    val dividendYield: Double? = null,
    val eps: Double? = null,
    val bookValue: Double? = null,
    val evToEbitda: Double? = null
)

@Serializable
data class Profitability(
    val roa: Double? = null,
    val roce: Double? = null,
    val roic: Double? = null,
    val grossMargin: Double? = null,
    val netMargin: Double? = null,
    val ebitdaMargin: Double? = null,
    val opMargin: Double? = null
)

@Serializable
data class RatioItem(
    val label: String,
    val value: Double?,
    val provenance: String
)

@Serializable
data class QuarterlyResult(
    val q: String,
    val rev: Double,
    val pat: Double,
    val eps: Double,
    val yoyRev: Double = 0.0
)

@Serializable
data class AnnualStatements(
    val pnl: Pnl = Pnl(),
    val balanceSheet: BalanceSheet = BalanceSheet(),
    val cashFlow: CashFlow = CashFlow()
)

@Serializable
data class Pnl(
    val revenue: Double = 0.0,
    val pat: Double = 0.0,
    val grossProfit: Double? = null,
    val operatingProfit: Double? = null
)

@Serializable
data class BalanceSheet(
    val totalAssets: Double = 0.0,
    val totalDebt: Double = 0.0,
    val equity: Double = 0.0,
    val currentAssets: Double? = null,
    val currentLiabilities: Double? = null
)

@Serializable
data class CashFlow(
    val ocf: Double = 0.0,
    val capex: Double = 0.0,
    val fcf: Double? = null,
    val opex: Double? = null
)

@Serializable
data class RedFlags(
    val earningsQuality: Boolean = false,
    val leverage: Boolean = false,
    val growth: Boolean = false,
    val management: Boolean = false
)

@Serializable
data class Shareholding(
    val promoter: Double = 0.0,
    val fii: Double = 0.0,
    val dii: Double = 0.0,
    val public: Double = 0.0
)

@Serializable
data class CorporateAction(
    val type: String = "",
    val date: String = "",
    val detail: String = ""
)

@Serializable
data class DcfEstimate(
    val wacc: Double = 0.0,
    val terminalGrowth: Double = 0.0,
    val equityValuePerShare: Double? = null,
    val upsidePercent: Double? = null
)
