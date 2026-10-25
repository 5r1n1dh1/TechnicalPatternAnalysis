package com.tpa.app.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface TpaApi {
    @GET("api/health") suspend fun health(): HealthResponse
    @GET("api/search") suspend fun search(@Query("q") q: String? = null, @Query("limit") limit: Int = 20): List<SearchResult>
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

// ---------------------------------------------------------------------------
// /api/health -> {"status":"ok","dataAsOf":null}
// ---------------------------------------------------------------------------
@Serializable
data class HealthResponse(
    val status: String = "",
    val dataAsOf: String? = null
)

// ---------------------------------------------------------------------------
// /api/search -> [{"symbol","name","sector"}]
// ---------------------------------------------------------------------------
@Serializable
data class SearchResult(val symbol: String = "", val name: String = "", val sector: String = "")

// ---------------------------------------------------------------------------
// /api/candles -> {"symbol","interval","range","tz","source","candles":[{t,o,h,l,c,v}]}
// ---------------------------------------------------------------------------
@Serializable
data class Candle(
    val t: String = "",
    val o: Double = 0.0,
    val h: Double = 0.0,
    val l: Double = 0.0,
    val c: Double = 0.0,
    val v: Double = 0.0
)

@Serializable
data class CandleResponse(
    val candles: List<Candle> = emptyList(),
    val symbol: String = "",
    val interval: String = "",
    val range: String = "",
    val tz: String = "",
    val source: String = ""
)

// ---------------------------------------------------------------------------
// /api/trend -> {"label","strength","ema20","ema50","adx"}
// ---------------------------------------------------------------------------
@Serializable
data class TrendResponse(
    val label: String = "",
    val strength: Int = 0,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val adx: Double = 0.0
)

// ---------------------------------------------------------------------------
// /api/patterns -> {"detections":[{"pattern","direction","confidence",
//                  "anchors":[{t,price}],"neckline":{t,price}}]}
// ---------------------------------------------------------------------------
@Serializable
data class PatternsResponse(val detections: List<Detection> = emptyList())

@Serializable
data class Detection(
    val pattern: String = "",
    val direction: String = "",
    val confidence: Double = 0.0,
    val anchors: List<Anchor> = emptyList(),
    val neckline: Neckline? = null,
    // Present in the trade-plan schema; optional on detections.
    val target_price: Double? = null,
    val stop_price: Double? = null,
    val risk_reward: Double? = null
) {
    /** The server does not send an id; derive a stable one for list keys/highlight. */
    val id: String get() = if (anchors.isEmpty()) pattern else "$pattern@${anchors.first().t}"

    val patternName: String get() = pattern
}

@Serializable
data class Anchor(val t: String = "", val price: Double = 0.0)

@Serializable
data class Neckline(val t: String = "", val price: Double = 0.0)

// ---------------------------------------------------------------------------
// /api/technicals -> {"rsi14","macdHistNorm","ema20","ema50","adx","atr14pct",
//   "bollingerWidthPct","volRatio520","deliveryPct5","s_r":{support,resistance},
//   "candleChips":[{pattern,date,strength}],"verdict":{bullish,bearish,neutral},
//   "trendBadge","structureChip"}
// ---------------------------------------------------------------------------
@Serializable
data class TechnicalResponse(
    val rsi14: Double = 0.0,
    val macdHistNorm: Double = 0.0,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val adx: Double = 0.0,
    val atr14pct: Double = 0.0,
    val bollingerWidthPct: Double = 0.0,
    val volRatio520: Double = 0.0,
    val deliveryPct5: Double? = null,
    val s_r: SrPayload = SrPayload(),
    val candleChips: List<CandleChip> = emptyList(),
    val verdict: Verdict = Verdict(),
    val trendBadge: String = "",
    val structureChip: String = ""
)

@Serializable
data class SrPayload(
    val support: List<SrLevel> = emptyList(),
    val resistance: List<SrLevel> = emptyList()
)

@Serializable
data class SrLevel(
    val price: Double = 0.0,
    val strength: Int = 0,
    val touches: Int = 0,
    val volume: Double = 0.0
)

@Serializable
data class Verdict(val bullish: Int = 0, val bearish: Int = 0, val neutral: Int = 0)

@Serializable
data class CandleChip(val pattern: String = "", val date: String = "", val strength: Int = 0)

// ---------------------------------------------------------------------------
// /api/decision -> {"verdict","confidence","components":{...},"reasons":[...]}
// ---------------------------------------------------------------------------
@Serializable
data class DecisionResponse(
    val verdict: String = "",
    val confidence: Double = 0.0,
    val components: DecisionComponents = DecisionComponents(),
    val reasons: List<String> = emptyList(),
    // Legacy hand-built fields (never sent by the server). Kept so existing
    // call sites that construct a DecisionResponse still compile; real data
    // always comes from `components`.
    val symbol: String = "",
    val component_scores: List<ComponentScore> = emptyList(),
    val failing_rubric: List<String> = emptyList(),
    val red_flags: List<RedFlag> = emptyList()
) {
    /** Component scores the UI can show, derived from the real `components` block. */
    val resolvedComponentScores: List<ComponentScore>
        get() = component_scores.ifEmpty {
            buildList {
                components.fundamental?.let { add(ComponentScore("Fundamental", it)) }
            }
        }

    /** Red flags the UI can show, derived from the real `components.redFlags`. */
    val resolvedRedFlags: List<RedFlag>
        get() = red_flags.ifEmpty {
            components.redFlags.filter { it.triggered }.map {
                RedFlag(title = it.name, detail = it.severity, severity = it.severity.uppercase())
            }
        }
}

@Serializable
data class DecisionComponents(
    val fundamental: Double? = null,
    val technical: TechnicalBlock = TechnicalBlock(),
    val valuation: ValuationBlock = ValuationBlock(),
    val pattern: PatternBlock = PatternBlock(),
    val ml: MlBlock = MlBlock(),
    val redFlags: List<RedFlagItem> = emptyList()
)

@Serializable
data class TechnicalBlock(
    val bearish: Boolean = false,
    val bullish: Boolean = false,
    val structureState: String? = null
)

@Serializable
data class ValuationBlock(val band: String = "", val moS_met: Boolean? = null)

@Serializable
data class PatternBlock(
    val bearishActive: Boolean = false,
    val bullishActive: Boolean = false,
    val confidence: Double = 0.0
)

@Serializable
data class MlBlock(
    val h1: Horizon = Horizon(),
    val h2: Horizon = Horizon(),
    val h5: Horizon = Horizon()
)

@Serializable
data class Horizon(val up: Double = 0.0, val down: Double = 0.0, val sideways: Double = 0.0)

@Serializable
data class RedFlagItem(val name: String = "", val triggered: Boolean = false, val severity: String = "")

@Serializable
data class ComponentScore(val component: String = "", val score: Double = 0.0)

@Serializable
data class RedFlag(val title: String = "", val detail: String = "", val severity: String = "")

// ---------------------------------------------------------------------------
// /api/fundamentals -> see server/api/fundamentals.py
// ---------------------------------------------------------------------------
@Serializable
data class FundamentalsResponse(
    val symbol: String = "",
    val asOf: String? = null,
    val header: Header = Header(),
    val valuation: Valuation = Valuation(),
    val profitability: Profitability = Profitability(),
    val quarterlyResults: List<QuarterlyResult> = emptyList(),
    val annualStatements: AnnualStatements = AnnualStatements(),
    val redFlags: RedFlags = RedFlags(),
    val shareholding: Shareholding = Shareholding(),
    // Not provided by the live server; retained for screen compatibility.
    val ratios: List<RatioItem> = emptyList(),
    val dcf: DcfEstimate = DcfEstimate(),
    val corporateActions: List<CorporateAction> = emptyList()
)

@Serializable
data class Header(
    val name: String = "",
    val sector: String = "",
    val mcap: Double = 0.0,
    // Server key is `fiftyTwoWRange`; property kept as w52Range for existing UI.
    @SerialName("fiftyTwoWRange") val w52Range: String = "",
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
    val q: String = "",
    val rev: Double = 0.0,
    val pat: Double = 0.0,
    val eps: Double = 0.0,
    val yoyRev: Double? = null
)

@Serializable
data class AnnualStatements(
    val pnl: Pnl = Pnl(),
    val balanceSheet: BalanceSheet = BalanceSheet(),
    val cashFlow: CashFlow = CashFlow()
)

@Serializable
data class Pnl(val revenue: Double? = null, val pat: Double? = null)

@Serializable
data class BalanceSheet(
    val totalAssets: Double? = null,
    val totalDebt: Double? = null,
    val equity: Double? = null
)

@Serializable
data class CashFlow(val ocf: Double? = null, val capex: Double? = null, val fcf: Double? = null)

@Serializable
data class RedFlags(
    val earningsQuality: Boolean = false,
    val leverage: Boolean = false,
    val growth: Boolean = false,
    val management: Boolean = false
)

@Serializable
data class Shareholding(
    val promoter: Double? = null,
    val fii: Double? = null,
    val dii: Double? = null,
    val public: Double? = null
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

// ---------------------------------------------------------------------------
// /api/predictions -> {"asOf","predictions":[{symbol,close,h1,h2,h5,metrics}],"skipped"}
// ---------------------------------------------------------------------------
@Serializable
data class PredictionsResponse(
    val asOf: String? = null,
    val predictions: List<PredictionItem> = emptyList(),
    val skipped: List<String> = emptyList()
)

@Serializable
data class PredictionItem(
    val symbol: String = "",
    val close: Double = 0.0,
    val h1: Horizon = Horizon(),
    val h2: Horizon = Horizon(),
    val h5: Horizon = Horizon()
)

// ---------------------------------------------------------------------------
// /api/scan -> {"asOf","hits":[...],"page","total"}
// ---------------------------------------------------------------------------
@Serializable
data class ScanResponse(
    val asOf: String? = null,
    val hits: List<ScanHit> = emptyList(),
    val page: Int = 1,
    val total: Int = 0
)

@Serializable
data class ScanHit(
    val symbol: String = "",
    val pattern: String = "",
    val direction: String = "",
    val confidence: Double = 0.0
)

// ---------------------------------------------------------------------------
// /api/macro -> {"asOf","vix","usdinr","crude","fiiDiiNet","gsec10y","provenance"}
// ---------------------------------------------------------------------------
@Serializable
data class MacroResponse(
    val asOf: String? = null,
    val vix: Double? = null,
    val usdinr: Double? = null,
    val crude: Double? = null,
    val fiiDiiNet: Double? = null,
    val gsec10y: Double? = null
)

// ---------------------------------------------------------------------------
// /api/tradeplan -> {"active":[{"pattern","entry":{t,price},"stop":{...},
//                     "target":{...},"rr","direction","invalidation","positionSize"}]}
// ---------------------------------------------------------------------------
@Serializable
data class TradePlanResponse(val active: List<ActivePlan> = emptyList())

@Serializable
data class ActivePlan(
    val pattern: String = "",
    val entry: PricePoint = PricePoint(),
    val stop: PricePoint = PricePoint(),
    val target: PricePoint = PricePoint(),
    val rr: Double = 0.0,
    val direction: String = "",
    val invalidation: String = "",
    val positionSize: Int = 0
)

@Serializable
data class PricePoint(val t: String = "", val price: Double = 0.0)
