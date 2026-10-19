package com.tpa.app.ui.chart

/** Chart candle interval, e.g. "1d", "1h", "5m". */
data class Interval(val value: String) {
    override fun toString(): String = value
}
