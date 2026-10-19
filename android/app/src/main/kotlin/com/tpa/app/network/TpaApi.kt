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