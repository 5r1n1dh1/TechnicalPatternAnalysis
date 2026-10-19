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