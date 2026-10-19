package com.tpa.app.ui.symbol

import com.tpa.app.network.Candle
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.Range

data class ChartState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val candles: List<Candle> = emptyList(),
    val symbol: String = "",
    val sector: String = "",
    val trend: String = "",
    val eodPrice: String = "",
    val interval: Interval = Interval("1d"),
    val range: Range = Range("1y")
)