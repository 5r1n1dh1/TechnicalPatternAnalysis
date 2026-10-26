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
 * Builds the MPAndroidChart line overlays for a detected pattern.
 * All coordinates are in chart space: X = candle index, Y = price.
 * The result is attached to the CombinedChart as its LineData component, so the
 * overlay renders above the candles and the volume bars.
 */
class PatternOverlayRenderer {

    private val colorGreenUp = Color.parseColor("#34D399")
    private val colorRedDown = Color.parseColor("#F87171")
    private val colorBgShade = Color.parseColor("#1A34D399")

    /**
     * Returns the overlay lines for [pattern], or null when there is nothing to draw.
     */
    fun buildOverlayData(pattern: PatternOverlay?): LineData? {
        if (pattern == null || pattern.anchors.isEmpty()) return null

        val overlayDataSets = mutableListOf<ILineDataSet>()

        // 1. Anchor polyline (solid line through all anchor points)
        val anchorEntries = pattern.anchors.map { p ->
            Entry(p.candleIndex.toFloat(), p.price.toFloat())
        }
        val anchorSet = LineDataSet(anchorEntries, "Anchor")
        anchorSet.color = colorGreenUp
        anchorSet.setDrawValues(false)
        anchorSet.setDrawCircles(false)
        anchorSet.lineWidth = 2f
        anchorSet.mode = LineDataSet.Mode.LINEAR
        overlayDataSets.add(anchorSet)

        // 2. Neckline (dashed line through the neckline anchor)
        pattern.neckline?.let { neckline ->
            val necklineEntries = listOf(
                Entry(neckline.startX, neckline.startY),
                Entry(neckline.endX, neckline.endY)
            )
            val necklineSet = LineDataSet(necklineEntries, "Neckline")
            necklineSet.color = colorRedDown
            necklineSet.setDrawValues(false)
            necklineSet.setDrawCircles(false)
            necklineSet.lineWidth = 1.5f
            necklineSet.enableDashedLine(8f, 4f, 0f)
            overlayDataSets.add(necklineSet)
        }

        // 3. Shaded zone between the pattern extremes
        val prices = pattern.anchors.map { it.price }
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        val leftX = pattern.anchors.first().candleIndex.toFloat()
        val rightX = pattern.anchors.last().candleIndex.toFloat()
        val shadedEntries = listOf(
            Entry(leftX, minPrice.toFloat()),
            Entry(leftX, maxPrice.toFloat()),
            Entry(rightX, maxPrice.toFloat()),
            Entry(rightX, minPrice.toFloat()),
            Entry(leftX, minPrice.toFloat())
        )
        val shadedSet = LineDataSet(shadedEntries, "Pattern Zone")
        shadedSet.setDrawValues(false)
        shadedSet.setDrawCircles(false)
        shadedSet.fillColor = colorBgShade
        shadedSet.setDrawFilled(true)
        shadedSet.color = colorBgShade
        shadedSet.setHighlightEnabled(false)
        overlayDataSets.add(shadedSet)

        // 4. Target line (green for bullish, red for bearish)
        pattern.targetPrice?.let { target ->
            val targetEntries = listOf(
                Entry(leftX, target.toFloat()),
                Entry(rightX, target.toFloat())
            )
            val targetSet = LineDataSet(targetEntries, "Target")
            targetSet.color = if (pattern.direction == "bull") colorGreenUp else colorRedDown
            targetSet.setDrawValues(false)
            targetSet.setDrawCircles(false)
            targetSet.lineWidth = 1.5f
            targetSet.enableDashedLine(8f, 4f, 0f)
            overlayDataSets.add(targetSet)
        }

        // 5. Stop line (red for bullish, green for bearish)
        pattern.stopPrice?.let { stop ->
            val stopEntries = listOf(
                Entry(leftX, stop.toFloat()),
                Entry(rightX, stop.toFloat())
            )
            val stopSet = LineDataSet(stopEntries, "Stop")
            stopSet.color = if (pattern.direction == "bull") colorRedDown else colorGreenUp
            stopSet.setDrawValues(false)
            stopSet.setDrawCircles(false)
            stopSet.lineWidth = 1.5f
            stopSet.enableDashedLine(8f, 4f, 0f)
            overlayDataSets.add(stopSet)
        }

        return LineData(overlayDataSets)
    }
}
