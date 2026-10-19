package com.tpa.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Query

interface TpaApi {
    @GET("api/health") suspend fun health(): HealthResponse
    @GET("api/search") suspend fun search(@Query("q") q: String? = null): List<SearchResult>
    @GET("api/candles") suspend fun getCandles(@Query("symbol") symbol: String, @Query("interval") interval: String, @Query("range") range: String): retrofit2.Response<CandleResponse>
    @GET("api/patterns") suspend fun getPatterns(@Query("symbol") symbol: String, @Query("interval") interval: String, @Query("range") range: String, @Query("pattern") pattern: String? = null): retrofit2.Response<PatternsResponse>
    @GET("api/trend") suspend fun getTrend(@Query("symbol") symbol: String, @Query("interval") interval: String, @Query("range") range: String): TrendResponse
    @GET("api/technicals") suspend fun getTechnicals(@Query("symbol") symbol: String): retrofit2.Response<TechnicalResponse>
    @GET("api/fundamentals") suspend fun getFundamentals(@Query("symbol") symbol: String): retrofit2.Response<FundamentalsResponse>
    @GET("api/predictions") suspend fun getPredictions(@Query("symbol") symbol: String): PredictionsResponse
    @GET("api/scan") suspend fun getScan(@Query("page") page: Int = 1): ScanResponse
    @GET("api/decision") suspend fun getDecision(@Query("symbol") symbol: String): DecisionResponse
    @GET("api/tradeplan") suspend fun getTradePlan(@Query("symbol") symbol: String, @Query("pattern") pattern: String? = null): retrofit2.Response<TradePlanResponse>
    @GET("api/macro") suspend fun getMacro(): MacroResponse
}

@Serializable
data class HealthResponse(
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

// Minimal DTO stubs for compile
@Serializable
data class SearchResult(val symbol: String = "", val name: String = "", val sector: String = "")

@Serializable
data class Candle(val t: String = "", val o: Double = 0.0, val h: Double = 0.0, val l: Double = 0.0, val c: Double = 0.0)

@Serializable
data class CandleResponse(val candles: List<Candle> = emptyList())

@Serializable
data class PatternsResponse(val patterns: List<Detection> = emptyList())

@Serializable
data class TrendResponse(val trend: String = "")

@Serializable
data class TechnicalResponse(
    val rsi: Double = 0.0,
    val macd: Double = 0.0,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val ema200: Double = 0.0,
    val adx: Double = 0.0,
    val plus_di: Double = 0.0,
    val minus_di: Double = 0.0,
    val atr_percent: Double = 0.0,
    val bb_width: Double = 0.0,
    val delivery_percent: Double = 0.0,
    val rvol: Double = 0.0,
    val structure: String = "",
    val verdict: Verdict = Verdict(),
    val outlook: MLOutlook = MLOutlook(),
    val sr_levels: List<SrLevel> = emptyList(),
    val candle_chips: List<CandleChip> = emptyList()
)

@Serializable
data class Verdict(val bullish: Boolean = false, val bearish: Boolean = false, val neutral: Boolean = false, val strength: Double = 0.0)
@Serializable
data class MLOutlook(val label: String = "", val confidence: Double = 0.0)
@Serializable
data class SrLevel(val level: String = "", val type: String = "", val price: Double = 0.0, val strength: Double = 0.0, val touches: Int = 0, val volume: Double = 0.0)
@Serializable
data class CandleChip(val label: String = "", val value: Double = 0.0, val indicator: String = "")

@Serializable
data class FundamentalsResponse(
    val header: Header = Header(),
    val valuation: Valuation = Valuation(),
    val profitability: Profitability = Profitability(),
    val ratios: List<RatioItem> = emptyList(),
    val redFlags: RedFlags = RedFlags(),
    val quarterlyResults: List<QuarterlyResult> = emptyList(),
    val shareholding: Shareholding = Shareholding(),
    val dcf: DcfEstimate = DcfEstimate(),
    val annualStatements: AnnualStatements = AnnualStatements(),
    val corporateActions: List<CorporateAction> = emptyList()
)

@Serializable
data class PredictionsResponse(val predictions: List<String> = emptyList())

@Serializable
data class ScanResponse(val results: List<String> = emptyList())

@Serializable
data class DecisionResponse(
    val symbol: String = "",
    val verdict: String = "",
    val confidence: Double = 0.0,
    val component_scores: List<ComponentScore> = emptyList(),
    val failing_rubric: List<String> = emptyList(),
    val red_flags: List<RedFlag> = emptyList(),
    val reasons: List<String> = emptyList()
)

@Serializable
data class ComponentScore(val component: String = "", val score: Double = 0.0)

@Serializable
data class RedFlag(val title: String = "", val detail: String = "", val severity: String = "")

@Serializable
data class TradePlanResponse(
    val entry_price: Double = 0.0,
    val entry_date: String = "",
    val stop_price: Double = 0.0,
    val stop_date: String = "",
    val target_price: Double = 0.0,
    val target_date: String = "",
    val risk_reward: Double = 0.0,
    val invalidation: String = "",
    val position_size: Double? = null
)

@Serializable
data class MacroResponse(val data: String = "")

@Serializable
data class Detection(
    val id: String = "",
    val anchors: List<Anchor> = emptyList(),
    val neckline: Neckline? = null,
    val target_price: Double = 0.0,
    val stop_price: Double = 0.0,
    val patternName: String = "",
    val direction: String = "",
    val confidence: Double = 0.0,
    val risk_reward: Double = 0.0
)

@Serializable
data class Anchor(val candle_index: Int = 0, val price: Double = 0.0)

@Serializable
data class Neckline(val candle_index: Int = 0, val price: Double = 0.0)
