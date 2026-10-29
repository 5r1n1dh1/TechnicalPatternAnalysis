package com.tpa.app.ui.chart

import android.graphics.Color
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

data class AnchorPoint(
    val candleIndex: Int,
    val price: Double
)

data class NecklineInfo(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

data class PatternOverlay(
    val anchors: List<AnchorPoint>,
    val neckline: NecklineInfo?,
    val targetPrice: Double?,
    val stopPrice: Double?,
    val patternName: String = "",
    val direction: String = "bull",
    val confidence: Double = 0.0,
    val riskReward: Double = 0.0
)

/**
 * Builds the MPAndroidChart line overlay for a detected pattern: one polyline
 * through the anchors, colored by direction, with a visible dot per anchor.
 * Horizontal levels (neckline, target, stop) are drawn as labeled axis limit
 * lines by the chart, so they stay readable at any zoom.
 */
class PatternOverlayRenderer {

    private val colorGreenUp = Color.parseColor("#34D399")
    private val colorRedDown = Color.parseColor("#F87171")

    fun buildOverlayData(pattern: PatternOverlay?): LineData? {
        if (pattern == null || pattern.anchors.isEmpty()) return null

        val color = if (pattern.direction == "bull") colorGreenUp else colorRedDown

        val anchorEntries = pattern.anchors.map { point ->
            Entry(point.candleIndex.toFloat(), point.price.toFloat())
        }
        val anchorSet = LineDataSet(anchorEntries, "Pattern anchors").apply {
            this.color = color
            lineWidth = 2f
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            setDrawCircles(true)
            setCircleColors(listOf(color))
            circleRadius = 3.5f
            setDrawCircleHole(false)
            setHighlightEnabled(false)
        }

        return LineData(listOf<ILineDataSet>(anchorSet))
    }
}
