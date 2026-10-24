package com.tpa.app.ui.symbol

import com.tpa.app.network.ActivePlan
import com.tpa.app.network.Candle
import com.tpa.app.network.Detection
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.Range

data class ChartState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val candles: List<Candle> = emptyList(),
    val symbol: String = "",
    val sector: String = "",
    val trend: String = "",
    val trendStrength: Int = 0,
    val eodPrice: String = "",
    val interval: Interval = Interval("1d"),
    // Server range enum is uppercase ("1Y"); lowercase breaks /candles.
    val range: Range = Range("1Y"),
    val selectedPattern: String? = null,
    val detections: List<Detection> = emptyList(),
    val tradePlan: ActivePlan? = null,
    val patternsLoading: Boolean = false,
    val patternsError: String? = null,
    val tradePlanLoading: Boolean = false,
    val tradePlanError: String? = null
)
