package com.tpa.app.ui.chart

/** Chart date range, e.g. "1d", "1w", "1Y", "5Y". */
data class Range(val value: String) {
    override fun toString(): String = value
}
