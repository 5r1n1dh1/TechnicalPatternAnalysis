package com.tpa.app.ui.symbol

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.utils.Utils
import com.tpa.app.network.Candle
import com.tpa.app.network.Detection
import com.tpa.app.ui.chart.AnchorPoint
import com.tpa.app.ui.chart.ChartFilters
import com.tpa.app.ui.chart.DetectionChip
import com.tpa.app.ui.chart.NecklineInfo
import com.tpa.app.ui.chart.PivotMarker
import com.tpa.app.ui.chart.PivotSummaryCard
import com.tpa.app.ui.chart.PatternOverlay
import com.tpa.app.ui.chart.PatternOverlayRenderer
import com.tpa.app.ui.components.GuideButton
import com.tpa.app.ui.theme.glassCard
import com.tpa.app.ui.theme.glassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val labelFormat = SimpleDateFormat("dd MMM", Locale.US)
private val clockLabelFormat = SimpleDateFormat("HH:mm", Locale.US)
private val intradayLabelFormat = SimpleDateFormat("dd MMM HH:mm", Locale.US)

/** Server pattern enums for the optional detection filter; null means all. */
private val PatternFilters = listOf<Pair<String?, String>>(
    null to "All",
    "head_and_shoulders" to "H&S",
    "cup_and_handle" to "Cup & handle",
    "double_triple" to "Double / triple",
    "flag" to "Flag"
)

@Composable
fun ChartTab(viewModel: ChartViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    val error = state.error
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.candles.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            error != null && state.candles.isEmpty() -> ErrorSurface(
                error = error,
                onRetry = { viewModel.fetchCandles() },
                onReset = viewModel::resetToDefault
            )

            else -> ChartContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun ChartContent(
    state: ChartState,
    viewModel: ChartViewModel
) {
    val selectedDetection = remember(state.selectedPattern, state.detections) { derivedStateOf { state.detections.firstOrNull { it.id == state.selectedPattern } } }.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.trend.isNotBlank()) {
                val t = state.trend.lowercase(Locale.US)
                val trendColor = when {
                    t.contains("up") || t.contains("bull") -> MaterialTheme.colorScheme.primary
                    t.contains("down") || t.contains("bear") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val trendText = if (state.trendStrength > 0) "${state.trend} ${state.trendStrength}%" else state.trend
                Text(
                    text = trendText,
                    color = trendColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            if (state.sector.isNotBlank()) {
                Text(
                    text = state.sector,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.eodPrice.isNotBlank()) {
                Text(
                    text = "EOD ${state.eodPrice}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            GuideButton("Chart terms", "chart", "eod", "trend", "sector")
        }
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        ChartFilters(
            range = state.range,
            interval = state.interval,
            onRangeChange = viewModel::updateRange,
            onIntervalChange = viewModel::updateInterval
        )

        selectedDetection?.let { detection ->
            OverlayLegend(detection)
        }

        // Candlestick chart.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .glassCard()
                .padding(6.dp)
        ) {
            if (state.candles.isEmpty()) {
                Text(
                    text = "No candle data for ${state.symbol}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                CandleStickCard(
                    candles = state.candles,
                    selected = selectedDetection,
                    interval = state.interval.value,
                    range = state.range.value,
                    upColor = MaterialTheme.colorScheme.primary.toArgb(),
                    downColor = MaterialTheme.colorScheme.error.toArgb(),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
                    gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f).toArgb(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (selectedDetection == null && state.detections.isNotEmpty()) {
            Text(
                text = "Tap a detection below to draw it on the chart.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        selectedDetection?.let { detection ->
            PivotSummaryCard(
                detection = detection,
                candles = state.candles,
                interval = state.interval.value,
                range = state.range.value
            )
        }

        // Detections.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Pattern Detections",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            if (state.patternsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.weight(1f))
            GuideButton("Pattern detections", "detection", "confidence", "rr", "overlay", "pivots")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PatternFilters) { (value, label) ->
                FilterChip(
                    selected = state.patternFilter == value,
                    onClick = { viewModel.setPatternFilter(value) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }
        if (state.patternsLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Scanning patterns on ${state.interval.value} · ${state.range.value}…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        state.patternsError?.let { message ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.loadPatterns() }) { Text("Retry") }
                    TextButton(onClick = { viewModel.resetToDefault() }) { Text("Reset 1d · 1Y") }
                }
            }
        }
        if (state.detections.isEmpty() && !state.patternsLoading) {
            Text("No patterns detected.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.detections) { detection ->
                    DetectionChip(
                        detection = detection,
                        selected = state.selectedPattern == detection.id,
                        onClick = { viewModel.selectPattern(detection.id, detection.patternName) }
                    )
                }
            }
        }

        // Trade plan for the selected detection.
        state.tradePlan?.let { plan ->
            Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (plan.pattern.isNotBlank()) "Trade Plan · ${plan.pattern}" else "Trade Plan",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "R:R ${String.format(Locale.US, "%.2f", plan.rr)}",
                            color = if (plan.rr >= 3) MaterialTheme.colorScheme.primary else if (plan.rr >= 2) Color(0xFFFBBF24) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        GuideButton("Trade plan", "entry", "stop", "target", "invalidation", "position", "rr")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlanMetric("Entry", plan.entry.price, MaterialTheme.colorScheme.primary)
                    PlanMetric("Stop", plan.stop.price, MaterialTheme.colorScheme.error)
                    PlanMetric("Target", plan.target.price, MaterialTheme.colorScheme.primary)
                }
                if (plan.invalidation.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Invalidation: ${plan.invalidation}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                if (plan.positionSize > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Position size: ${plan.positionSize}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
        if (state.tradePlanLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Building trade plan…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        state.tradePlanError?.let {
            Text("Trade plan: $it", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlanMetric(label: String, value: Double, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorSurface(error: String, onRetry: () -> Unit, onReset: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().glassCard().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Chart unavailable", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry) { Text("Retry") }
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Reset 1d · 1Y")
                }
            }
        }
    }
}

@Composable
private fun OverlayLegend(detection: Detection) {
    val isBull = detection.direction.lowercase(Locale.US).startsWith("bull")
    val directionColor = if (isBull) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier.fillMaxWidth().glassSurface().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = detection.patternName.replace('_', ' ').replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = (if (isBull) "Bullish" else "Bearish") +
                    " · " + String.format(Locale.US, "%.0f", detection.confidence) + "%",
                fontSize = 11.sp,
                color = directionColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch("Anchors", directionColor, filled = true)
            LegendSwatch("Neckline", Color(0xFFFBBF24), filled = false)
            if (detection.target_price != null) LegendSwatch("Target", directionColor, filled = false)
            if (detection.stop_price != null) {
                LegendSwatch(
                    "Stop",
                    if (isBull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    filled = false
                )
            }
        }
        if (detection.pivots.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendSwatch("High pivot", PivotMarker.highColor, filled = true)
                LegendSwatch("Low pivot", PivotMarker.lowColor, filled = true)
            }
        }
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (filled) 7.dp else 9.dp)
                .background(color, if (filled) CircleShape else RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CandleStickCard(
    candles: List<Candle>,
    selected: Detection?,
    interval: String,
    range: String,
    upColor: Int,
    downColor: Int,
    textColor: Int,
    gridColor: Int,
    modifier: Modifier = Modifier
) {
    val labelRef = remember { object { var value = emptyList<String>() } }
    val sigRef = remember { object { var value = "" } }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CombinedChart(ctx).apply {
                description = null
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setTouchEnabled(true)
                isScaleXEnabled = true
                isScaleYEnabled = true
                isHighlightPerTapEnabled = true
                isDoubleTapToZoomEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.textColor = textColor
                axisLeft.setDrawAxisLine(false)
                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = gridColor
                axisLeft.gridLineWidth = 0.5f
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.setDrawAxisLine(false)
                xAxis.textColor = textColor
                xAxis.granularity = 1f
                xAxis.setLabelCount(4, false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return labelRef.value.getOrNull(value.toInt()) ?: ""
                    }
                }
            }
        },
        update = { chart ->
            // T-47: digest volume sum so the BarChart below redraws on data change.
            val volSum = candles.sumOf { (it.v ?: 0.0).toDouble() }.toInt()
            val newSignature = buildString {
                append(candles.size).append('|')
                append(candles.firstOrNull()?.t).append('|')
                append(candles.lastOrNull()?.t).append('|')
                append(selected?.id).append('|')
                append(interval).append('|').append(range).append('|')
                append(selected?.pivots?.joinToString { "${it.idx}:${it.kind}:${it.price}" }).append('|')
                append(upColor).append('|').append(downColor).append('|')
                append(textColor).append('|').append(gridColor).append('|')
                append("v").append(volSum).append('|')
                append("n").append(selected?.neckline?.price).append('|')
                append("t").append(selected?.target_price).append('|')
                append("s").append(selected?.stop_price)
            }
            if (sigRef.value == newSignature) return@AndroidView
            sigRef.value = newSignature

            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.textColor = textColor

            val entries = ArrayList<CandleEntry>(candles.size)
            val newLabels = ArrayList<String>(candles.size)
            candles.forEachIndexed { index, candle ->
                entries.add(
                    CandleEntry(
                        index.toFloat(),
                        candle.h.toFloat(),
                        candle.l.toFloat(),
                        candle.o.toFloat(),
                        candle.c.toFloat()
                    )
                )
                newLabels.add(formatLabel(candle.t, interval, range))
            }
            labelRef.value = newLabels

            // Volume bars (T-47): real v values are scaled into the lower band of the
            // price range so they share the left axis with the candles instead of
            // crowding it. Scaling is linear, so bar heights stay proportional.
            val priceMin = candles.minOf { it.l.toDouble() }
            val priceMax = candles.maxOf { it.h.toDouble() }
            val band = ((priceMax - priceMin).coerceAtLeast(0.01)) * 0.2
            val maxVolume = candles.maxOf { (it.v ?: 0.0).toDouble() }.coerceAtLeast(1.0)
            val volumeScale = band / maxVolume
            val volumeEntries = ArrayList<BarEntry>(candles.size)
            candles.forEachIndexed { idx, candle ->
                val scaled = priceMin + (candle.v ?: 0.0).toDouble() * volumeScale
                volumeEntries.add(BarEntry(idx.toFloat(), scaled.toFloat()))
            }
            val volDataSet = BarDataSet(volumeEntries, "Volume").apply {
                setDrawValues(false)
                color = AndroidColor.argb(70, 148, 163, 184)
            }
            val priceDataSet = com.github.mikephil.charting.data.CandleDataSet(entries, "Price").apply {
                setDrawValues(false)
                increasingColor = upColor
                decreasingColor = downColor
                neutralColor = textColor
                shadowColorSameAsCandle = true
                barSpace = 0.08f
                shadowWidth = 0.7f
            }
            val priceData = com.github.mikephil.charting.data.CandleData(priceDataSet)
            val volumeData = BarData(volDataSet).apply { barWidth = 0.3f }

            // Pattern levels as labeled horizontal lines: neckline, target and stop.
            chart.axisLeft.removeAllLimitLines()
            selected?.neckline?.price?.let { price ->
                chart.axisLeft.addLimitLine(
                    levelLine("Neckline " + formatPrice(price), price.toFloat(), 0xFFFBBF24.toInt(), textColor, true)
                )
            }
            // Bullish setups target higher and stop lower; bearish setups invert.
            val isBull = selected?.direction?.lowercase(Locale.US)?.startsWith("bull") == true
            val targetColor = if (isBull) upColor else downColor
            val stopColor = if (isBull) downColor else upColor
            selected?.target_price?.let { price ->
                chart.axisLeft.addLimitLine(
                    levelLine("Target " + formatPrice(price), price.toFloat(), targetColor, textColor, false)
                )
            }
            selected?.stop_price?.let { price ->
                chart.axisLeft.addLimitLine(
                    levelLine("Stop " + formatPrice(price), price.toFloat(), stopColor, textColor, false)
                )
            }

            // T-48: direction-colored anchor path with visible pivot dots.
            val overlay = selected?.let { toOverlay(it, candles) }
            val overlayData = PatternOverlayRenderer().buildOverlayData(overlay)

            // T-49: pivot markers split into HIGH (amber) and LOW (blue) dots.
            val scatterData = selected?.pivots?.let { pivots ->
                fun entries(kind: String): List<Entry> = pivots
                    .filter { it.kind.equals(kind, ignoreCase = true) }
                    .mapNotNull { p ->
                        val idx = pointIndex(p.idx.takeIf { it >= 0 }, p.t, candles)
                        if (idx >= 0) Entry(idx.toFloat(), p.price.toFloat()) else null
                    }

                fun dataSet(entries: List<Entry>, label: String, color: Int): ScatterDataSet =
                    ScatterDataSet(entries, label).apply {
                        setDrawValues(false)
                        setHighlightEnabled(false)
                        this.color = color
                        setScatterShapeHoleColor(PivotMarker.glass.toArgb())
                        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                        setScatterShapeSize(7f)
                    }

                val high = entries("HIGH")
                val low = entries("LOW")
                if (high.isEmpty() && low.isEmpty()) {
                    null
                } else {
                    val sets = ArrayList<IScatterDataSet>(2)
                    if (high.isNotEmpty()) sets.add(dataSet(high, "High pivots", PivotMarker.highColor.toArgb()))
                    if (low.isNotEmpty()) sets.add(dataSet(low, "Low pivots", PivotMarker.lowColor.toArgb()))
                    ScatterData(sets)
                }
            }

            // CombinedChart carries price candles, volume bars, overlay lines and pivots.
            val combined = CombinedData()
            combined.setData(priceData)
            combined.setData(volumeData)
            if (overlayData != null) combined.setData(overlayData)
            if (scatterData != null) combined.setData(scatterData)
            chart.data = combined

            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}

private fun formatPrice(price: Double): String = String.format(Locale.US, "%.2f", price)

private fun levelLine(
    label: String,
    price: Float,
    color: Int,
    textColor: Int,
    dashed: Boolean
): LimitLine = LimitLine(price, label).apply {
    lineColor = color
    lineWidth = 1.2f
    // `this.` is required: the function parameters shadow the inherited members.
    this.textColor = textColor
    this.textSize = Utils.convertDpToPixel(9f)
    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
    if (dashed) enableDashedLine(10f, 6f, 0f)
}

/** Prefer the server bar index; fall back to timestamp matching for older payloads. */
private fun pointIndex(idx: Int?, t: String, candles: List<Candle>): Int {
    if (idx != null && idx in candles.indices) return idx
    return indexForTime(t, candles)
}

/** Server anchor points carry a timestamp; map it back to a candle index for the overlay. */
private fun indexForTime(t: String, candles: List<Candle>): Int {
    val exact = candles.indexOfFirst { it.t == t }
    if (exact >= 0) return exact
    if (t.isBlank()) return -1
    val day = t.take(10)
    return candles.indexOfFirst { cand -> cand.t.take(10) == day }
}

private fun toOverlay(detection: Detection, candles: List<Candle>): PatternOverlay {
    val anchors = detection.anchors.mapNotNull { anchor ->
        val idx = pointIndex(anchor.idx, anchor.t, candles)
        if (idx >= 0) AnchorPoint(idx, anchor.price) else null
    }
    val neckline = detection.neckline?.let { n ->
        val idx = pointIndex(n.idx, n.t, candles)
        if (idx >= 0) {
            NecklineInfo(idx.toFloat(), n.price.toFloat(), candles.lastIndex.toFloat(), n.price.toFloat())
        } else {
            null
        }
    }
    val isBull = detection.direction.lowercase(Locale.US).startsWith("bull")
    return PatternOverlay(
        anchors = anchors,
        neckline = neckline,
        targetPrice = detection.target_price,
        stopPrice = detection.stop_price,
        patternName = detection.patternName,
        direction = if (isBull) "bull" else "bear",
        confidence = detection.confidence,
        riskReward = detection.risk_reward ?: 0.0
    )
}

private fun formatLabel(t: String, interval: String, range: String): String {
    val parsed: Date? = try {
        isoFormat.parse(t)
    } catch (e: Exception) {
        null
    } ?: try {
        dayFormat.parse(t)
    } catch (e: Exception) {
        null
    }
    val formatter = when {
        range == "1D" -> clockLabelFormat
        interval == "5m" || interval == "15m" || interval == "30m" || interval == "1h" ->
            intradayLabelFormat
        else -> labelFormat
    }
    return parsed?.let { formatter.format(it) } ?: t
}
