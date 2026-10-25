package com.tpa.app.ui.chart

import android.graphics.Color
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
 * MPAndroidChart overlay renderer that draws pattern overlays on top of candlestick charts.
 * All coordinates are in chart space: X = candle index, Y = price.
 * Uses separate LineDataSet overlays instead of modifying the candle dataset for fast refresh.
 */
class PatternOverlayRenderer(
    private val chart: CandleStickChart,
    private val viewPortHandler: ViewPortHandler
) {

    private val colorGreenUp = Color.parseColor("#34D399")
    private val colorRedDown = Color.parseColor("#F87171")
    private val colorAmber = Color.parseColor("#FBBF24")
    private val colorBgShade = Color.parseColor("#1A34D399")

    private val overlayDataSets = mutableListOf<LineDataSet>()

    init {
        chart.apply {
            setTouchEnabled(true)
            description = null
            isScaleXEnabled = true
            isScaleYEnabled = true
            isHighlightPerTapEnabled = true
        }
    }

    /**
     * Draws all pattern overlay elements on the chart.
     * Clears previous overlays before redrawing.
     */
    fun drawPatternOverlay(pattern: PatternOverlay?) {
        clearOverlays()
        if (pattern == null) {
            val combined = chart.data as? com.github.mikephil.charting.data.CombinedData
            combined?.lineData = null
            chart.invalidate()
            return
        }

        val candleData = chart.data as? CandleData
        val lineData = chart.data as? LineData ?: LineData()

        // 1. Anchor polyline (solid line through all anchor points)
        val anchorEntries = pattern.anchors.mapIndexed { i, p ->
            Entry(p.candleIndex.toFloat(), p.price.toFloat())
        }
        if (anchorEntries.isNotEmpty()) {
            val anchorSet = LineDataSet(anchorEntries, "Anchor")
            anchorSet.color = colorGreenUp
            anchorSet.setDrawValues(false)
            anchorSet.setDrawCircles(false)
            anchorSet.lineWidth = 2f
            anchorSet.mode = LineDataSet.Mode.LINEAR
            overlayDataSets.add(anchorSet)
        }

        // 2. Neckline (dashed horizontal/sloped line)
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

        // 3. Shaded zone between pattern extremes
        val prices = pattern.anchors.map { it.price }
        if (prices.isNotEmpty()) {
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
            val shadedSet = LineDataSet(shadedEntries, "Shaded Zone")
            shadedSet.setDrawValues(false)
            shadedSet.setDrawCircles(false)
            shadedSet.fillColor = colorBgShade
            shadedSet.setDrawFilled(true)
            shadedSet.setColor(colorBgShade)
            shadedSet.setHighlightEnabled(false)
            overlayDataSets.add(shadedSet)
        }

        // 4. Target line (dashed green for bullish, red for bearish)
        pattern.targetPrice?.let { target ->
            val targetEntries = listOf(
                Entry(pattern.anchors.first().candleIndex.toFloat(), target.toFloat()),
                Entry(pattern.anchors.last().candleIndex.toFloat(), target.toFloat())
            )
            val targetSet = LineDataSet(targetEntries, "Target")
            targetSet.color = if (pattern.direction == "bull") colorGreenUp else colorRedDown
            targetSet.setDrawValues(false)
            targetSet.setDrawCircles(false)
            targetSet.lineWidth = 1.5f
            targetSet.enableDashedLine(8f, 4f, 0f)
            overlayDataSets.add(targetSet)
        }

        // 5. Stop line (dashed red/green opposite of target)
        pattern.stopPrice?.let { stop ->
            val stopEntries = listOf(
                Entry(pattern.anchors.first().candleIndex.toFloat(), stop.toFloat()),
                Entry(pattern.anchors.last().candleIndex.toFloat(), stop.toFloat())
            )
            val stopSet = LineDataSet(stopEntries, "Stop")
            stopSet.color = if (pattern.direction == "bull") colorRedDown else colorGreenUp
            stopSet.setDrawValues(false)
            stopSet.setDrawCircles(false)
            stopSet.lineWidth = 1.5f
            stopSet.enableDashedLine(8f, 4f, 0f)
            overlayDataSets.add(stopSet)
        }

        // T-48: attach drawn overlays to CombinedChart via its lineData so markers render over candles+volume
        val combined = chart.data as? com.github.mikephil.charting.data.CombinedData
        if (combined != null && overlayDataSets.isNotEmpty()) {
            val shared = com.github.mikephil.charting.data.LineData(overlayDataSets)
            combined.lineData = shared
        } else if (combined == null) {
            // Fallback for standalone CandleStickChart: attach directly
            val lineData = com.github.mikephil.charting.data.LineData(overlayDataSets)
            (chart.data as? com.github.mikephil.charting.data.CandleData)?.also { cd -> cd.extra = lineData }
        }
        chart.invalidate()
    }

    fun clearOverlays() {
        overlayDataSets.forEach { it.clear() }
        overlayDataSets.clear()
        val combined = chart.data as? com.github.mikephil.charting.data.CombinedData
        combined?.lineData = null
        chart.invalidate()
    }

    companion object {
        fun CoroutineScope.drawPatternOverlayAsync(
            chart: CandleStickChart,
            pattern: PatternOverlay?
        ) {
            launch(Dispatchers.Main) {
                val renderer = PatternOverlayRenderer(
                    chart,
                    chart.viewPortHandler
                )
                renderer.drawPatternOverlay(pattern)
            }
        }
    }
}
