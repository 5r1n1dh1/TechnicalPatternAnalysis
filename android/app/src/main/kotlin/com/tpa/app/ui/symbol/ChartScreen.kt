package com.tpa.app.ui.symbol

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.tpa.app.R
import com.tpa.app.network.Candle
import com.tpa.app.network.Detection
import com.tpa.app.ui.chart.AnchorPoint
import com.tpa.app.ui.chart.DetectionChip
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.NecklineInfo
import com.tpa.app.ui.chart.PatternDropdown
import com.tpa.app.ui.chart.PivotMarker
import com.tpa.app.ui.chart.PivotPoint
import com.tpa.app.ui.chart.PatternOverlay
import com.tpa.app.ui.chart.PatternOverlayRenderer
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.RangeDropdown
import com.tpa.app.ui.theme.glassCard
import com.tpa.app.ui.theme.glassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEFAULT_SYMBOL = "RELIANCE"

private val UpColor = Color(0xFF34D399)
private val DownColor = Color(0xFFF87171)
private val MutedColor = Color(0xFF94A3B8)

/** All intervals the API accepts, in ascending order. */
private val AllIntervals = listOf("5m", "15m", "30m", "1h", "1d", "1w", "1M")

private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val labelFormat = SimpleDateFormat("dd MMM", Locale.US)

@Composable
fun ChartScreen(viewModel: ChartViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    // The ViewModel starts with no symbol; load a default so the tab is usable immediately.
    LaunchedEffect(Unit) {
        if (state.symbol.isBlank()) viewModel.setSymbol(DEFAULT_SYMBOL)
    }

    val error = state.error
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            error != null -> ErrorSurface(error = error, onRetry = { viewModel.fetchCandles() })

            else -> ChartContent(state = state, viewModel = viewModel, onBack = onBack)
        }
    }
}

@Composable
private fun ChartContent(
    state: ChartState,
    viewModel: ChartViewModel,
    onBack: () -> Unit
) {
    val validIntervals = validIntervalsFor(state.range)
    val selectedDetection = state.detections.firstOrNull { it.id == state.selectedPattern }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: symbol identity + quick stats.
        Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.symbol.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                if (state.trend.isNotBlank()) {
                    val t = state.trend.lowercase(Locale.US)
                    val trendColor = when {
                        t.contains("up") || t.contains("bull") -> UpColor
                        t.contains("down") || t.contains("bear") -> DownColor
                        else -> MutedColor
                    }
                    val trendText = if (state.trendStrength > 0) "${state.trend} ${state.trendStrength}%" else state.trend
                    Text(
                        text = trendText,
                        color = trendColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Back") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.sector.isNotBlank()) {
                    Text(state.sector, color = MutedColor, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                }
                if (state.eodPrice.isNotBlank()) {
                    Text("EOD ${state.eodPrice}", color = MutedColor, fontSize = 12.sp)
                }
            }
        }

        // Symbol switcher.
        var symbolInput by remember { mutableStateOf(state.symbol) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = symbolInput,
                onValueChange = { symbolInput = it.uppercase(Locale.US) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Symbol") }
            )
            Button(
                onClick = { if (symbolInput.isNotBlank()) viewModel.setSymbol(symbolInput) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Load")
            }
        }

        // Filters.
        PatternDropdown(symbol = state.symbol) { pattern ->
            viewModel.selectPattern(pattern)
            viewModel.loadPatterns()
        }

        RangeDropdown(symbol = state.symbol, range = state.range) { range ->
            viewModel.updateRange(range)
            // Keep the interval valid for the newly selected range.
            val allowed = validIntervalsFor(range)
            if (allowed.none { it.value == state.interval.value }) {
                allowed.firstOrNull()?.let { viewModel.updateInterval(it) }
            }
        }

        IntervalSelector(
            interval = state.interval,
            validIntervals = validIntervals,
            onIntervalChange = { viewModel.updateInterval(it) }
        )

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
                    color = MutedColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                CandleStickCard(
                    candles = state.candles,
                    selected = selectedDetection,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Detections.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Pattern Detections",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            if (state.patternsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }
        state.patternsError?.let {
            Text("Patterns: $it", color = DownColor, fontSize = 11.sp)
        }
        if (state.detections.isEmpty() && !state.patternsLoading) {
            Text("No patterns detected.", color = MutedColor, fontSize = 12.sp)
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "R:R ${String.format(Locale.US, "%.2f", plan.rr)}",
                        color = if (plan.rr >= 3) UpColor else if (plan.rr >= 2) Color(0xFFFBBF24) else DownColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlanMetric("Entry", plan.entry.price, UpColor)
                    PlanMetric("Stop", plan.stop.price, DownColor)
                    PlanMetric("Target", plan.target.price, UpColor)
                }
                if (plan.invalidation.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Invalidation: ${plan.invalidation}", color = MutedColor, fontSize = 11.sp)
                }
                if (plan.positionSize > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Position size: ${plan.positionSize}", color = MutedColor, fontSize = 11.sp)
                }
            }
        }
        if (state.tradePlanLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
        state.tradePlanError?.let {
            Text("Trade plan: $it", color = DownColor, fontSize = 11.sp)
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlanMetric(label: String, value: Double, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MutedColor)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorSurface(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().glassCard().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Chart unavailable", color = DownColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = error,
                color = MutedColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Interval picker. [IntervalDropdown] keeps its validity matrix private (and keyed by a
 * different range vocabulary), so the design-doc matrix from design/plan.md is implemented
 * locally and only the intervals valid for [range] are offered.
 */
@Composable
private fun IntervalSelector(
    interval: Interval,
    validIntervals: List<Interval>,
    onIntervalChange: (Interval) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Interval: ${interval.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_menu_down),
                    contentDescription = "Select interval",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            validIntervals.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.value) },
                    onClick = {
                        onIntervalChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Interval validity matrix from design/plan.md. Range values come from [RangeDropdown]
 * ("1D".."10Y"); only combinations accepted by the API are returned.
 */
private fun validIntervalsFor(range: Range): List<Interval> {
    val allowed = when (range.value.uppercase(Locale.US)) {
        "1D" -> setOf("5m", "15m", "30m", "1h")
        "5D", "1M" -> setOf("5m", "15m", "30m", "1h", "1d")
        "3M", "6M" -> setOf("1h", "1d")
        "1Y" -> setOf("1h", "1d", "1w", "1M")
        "2Y", "5Y", "10Y" -> setOf("1d", "1w", "1M")
        else -> setOf("1d")
    }
    return AllIntervals.filter { it in allowed }.map { Interval(it) }
}

@Composable
private fun CandleStickCard(
    candles: List<Candle>,
    selected: Detection?,
    modifier: Modifier = Modifier
) {
    val labels = remember { mutableStateOf<List<String>>(emptyList()) }
    val signature = remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CandleStickChart(ctx).apply {
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
                axisLeft.textColor = AndroidColor.parseColor("#94A3B8")
                axisLeft.setDrawGridLines(false)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.textColor = AndroidColor.parseColor("#94A3B8")
                xAxis.granularity = 1f
                xAxis.setLabelCount(4, false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return labels.value.getOrNull(value.toInt()) ?: ""
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
                append("v$volSum")
            }
            if (signature.value == newSignature) return@AndroidView
            signature.value = newSignature

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
                newLabels.add(formatLabel(candle.t))
            }
            labels.value = newLabels

            // Volume bars (T-47): real v in BarChart overlaying Candles (overlayed via a second
            // axis is out of scope here; volume shares left axis with candles so the color encoding
            // is visible even when values would otherwise crowd the axis).
            chart.axisRight.isEnabled = true
            val volumeEntries = ArrayList<com.github.mikephil.charting.data.BarEntry>(candles.size)
            candles.forEachIndexed { idx, candle ->
                volumeEntries.add(com.github.mikephil.charting.data.BarEntry(idx.toFloat(), (candle.v ?: 0.0f).toFloat()))
            }
            val volDataSet = com.github.mikephil.charting.data.BarDataSet(volumeEntries, "Volume").apply {
                setDrawValues(false)
                barWidth = 0.3f
                color = AndroidColor.parseColor("#64748B")
            }
            val priceDataSet = com.github.mikephil.charting.data.CandleDataSet(entries, "Price").apply {
                setDrawValues(false)
                increasingColor = AndroidColor.parseColor("#34D399")
                decreasingColor = AndroidColor.parseColor("#F87171")
                neutralColor = AndroidColor.parseColor("#94A3B8")
                shadowColorSameAsCandle = true
                barSpace = 0.08f
                shadowWidth = 0.7f
            }
            val priceData = com.github.mikephil.charting.data.CandleData(priceDataSet)
            val volumeData = com.github.mikephil.charting.data.BarData(volDataSet)
            // T-47: CombinedChart with price candles + volume bars below; volume synchronizes via shared x-index.
            chart.data = com.github.mikephil.charting.data.CombinedData(priceData, volumeData)
            val overlay = selected?.let { toOverlay(it, candles) }
            if (overlay != null && overlay.anchors.isNotEmpty()) {
                PatternOverlayRenderer(chart, chart.viewPortHandler).drawPatternOverlay(overlay)
            }

            // T-49: pivot markers (HIGH/LOW) rendered as scatter dots on selected detection
            selected?.pivots?.let { pivots ->
                val pivotEntries = pivots.mapNotNull { p ->
                    val idx = indexForTime(p.t, candles)
                    if (idx >= 0) com.github.mikephil.charting.data.Entry(idx.toFloat(), p.price.toFloat()) else null
                }
                if (pivotEntries.isNotEmpty()) {
                    val sds = com.github.mikephil.charting.data.ScatterDataSet(pivotEntries, "Pivots").apply {
                        setDrawValues(false)
                        setDrawFilled(true)
                        fillColor = PivotMarker.glass
                        color = PivotMarker.highColor
                        scatterShape = com.github.mikephil.charting.data.ScatterShape.CIRCLE
                        scatterShapeSize = 7f
                    }
                    chart.extraData = com.github.mikephil.charting.data.ScatterData(sds)
                }
            }

            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
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
        val idx = indexForTime(anchor.t, candles)
        if (idx >= 0) AnchorPoint(idx, anchor.price) else null
    }
    val neckline = detection.neckline?.let { n ->
        val idx = indexForTime(n.t, candles)
        if (idx >= 0) {
            NecklineInfo(idx.toFloat(), n.price.toFloat(), idx.toFloat(), n.price.toFloat())
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

private fun formatLabel(t: String): String {
    val parsed: Date? = try {
        isoFormat.parse(t)
    } catch (e: Exception) {
        null
    } ?: try {
        dayFormat.parse(t)
    } catch (e: Exception) {
        null
    }
    return parsed?.let { labelFormat.format(it) } ?: t
}
