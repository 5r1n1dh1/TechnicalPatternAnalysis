package com.tpa.app.network

import com.tpa.app.ui.symbol.ChartState
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.Range
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TpaApi {
    @GET("api/candles")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String
    ): Response<CandleResponse>

    @GET("api/resample")
    suspend fun getResampledData(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String
    ): Response<CandleResponse>

    @GET("api/patterns")
    suspend fun getPatterns(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("range") range: String,
        @Query("pattern") pattern: String
    ): Response<PatternsResponse>

    @GET("api/trade-plan")
    suspend fun getTradePlan(
        @Query("symbol") symbol: String,
        @Query("pattern") pattern: String
    ): Response<TradePlanResponse>

    @GET("api/technical")
    suspend fun getTechnical(
        @Query("symbol") symbol: String
    ): Response<TechnicalResponse>

    @GET("api/decision")
    suspend fun getDecision(
        @Query("symbol") symbol: String
    ): Response<DecisionResponse>
}

data class CandleResponse(
    val candles: List<Candle>
)

data class Candle(
    val time: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

data class PatternsResponse(
    val patterns: List<Detection>
)

data class Detection(
    val id: String,
    val patternName: String,
    val direction: String,
    val confidence: Double,
    val risk_reward: Double = 0.0,
    val anchors: List<AnchorPoint> = emptyList(),
    val neckline: AnchorPoint? = null,
    val target_price: Double? = null,
    val stop_price: Double? = null,
    val family: String = ""
)

data class AnchorPoint(
    val candle_index: Int,
    val price: Double
)

data class TradePlanResponse(
    val symbol: String,
    val pattern: String,
    val direction: String,
    val entry_price: Double,
    val entry_date: String,
    val stop_price: Double,
    val stop_date: String,
    val target_price: Double,
    val target_date: String,
    val risk_reward: Double,
    val invalidation: String,
    val position_size: Double? = null
)

data class TechnicalResponse(
    val symbol: String,
    val rsi: Double,
    val macd: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val adx: Double,
    val plus_di: Double,
    val minus_di: Double,
    val atr_percent: Double,
    val bb_width: Double,
    val delivery_percent: Double,
    val rvol: Double,
    val structure: String,
    val sr_levels: List<S_RLevel>,
    val candle_chips: List<CandleChip>,
    val verdict: Verdict,
    val outlook: MLOutlook
)

data class S_RLevel(
    val level: String,
    val type: String,
    val price: Double,
    val strength: String,
    val touches: Int,
    val volume: Double
)

data class CandleChip(
    val label: String,
    val value: String,
    val indicator: String
)

data class Verdict(
    val bullish: Int,
    val bearish: Int,
    val neutral: Int,
    val strength: String
)

data class MLOutlook(
    val label: String,
    val confidence: Double
)

data class DecisionResponse(
    val symbol: String,
    val verdict: String,
    val confidence: Double,
    val component_scores: List<ComponentScore>,
    val failing_rubric: List<String>,
    val red_flags: List<RedFlag>,
    val reasons: List<String>
)

data class ComponentScore(
    val component: String,
    val score: Double
)

data class RedFlag(
    val title: String,
    val detail: String
)