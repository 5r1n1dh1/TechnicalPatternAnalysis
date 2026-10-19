package com.tpa.app.ui.symbol

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.tpa.app.R
import com.tpa.app.ui.chart.IntervalDropdown
import com.tpa.app.ui.chart.RangeDropdown
import com.tpa.app.ui.chart.PatternDropdown
import com.tpa.app.network.NetworkModule
import com.tpa.app.network.TpaApi
import android.graphics.Color as AndroidColor
import com.tpa.app.ui.theme.glassSurface
import com.tpa.app.ui.chart.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.state.value
    var selectedTab by remember { mutableStateOf(1) } // 0=Fundamentals, 1=Charts, 2=Technicals
    
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else if (state.error != null) {
        Text(text = "Error: ${state.error}", modifier = Modifier.fillMaxSize())
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with symbol info
            SymbolScreen(
                symbol = state.symbol,
                sector = state.sector,
                trend = state.trend,
                eodPrice = state.eodPrice
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Fundamentals", "Charts", "Technicals").forEachIndexed { index, label ->
                    TextButton(
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            label,
                            color = if (selectedTab == index) Color(0xFF34D399) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 2) {
                val techVm = remember { TechnicalViewModel(NetworkModule.provideApi() as TpaApi) }
                val tech by techVm.technical.collectAsState(initial = null)
                val dec by techVm.decision.collectAsState(initial = null)
                LaunchedEffect(state.symbol) { techVm.load(state.symbol) }
                TechnicalScreen(technical = tech, decision = dec, modifier = Modifier.weight(1f))
            } else if (selectedTab == 0) {
                Text("Fundamentals", color = Color.White)
            } else {
                // Chart container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val chart = remember {
                    CandleStickChart(context)
                }
                
                chart.apply {
                    setTouchEnabled(true)
                    description = null
                    isScaleXEnabled = true
                    isScaleYEnabled = true
                    isHighlightPerTapEnabled = true
                    
                    // Configure candlestick chart
                    val colors = listOf(AndroidColor.GREEN, AndroidColor.RED)
                    
                    state.candles?.let { candles ->
                        val entries = ArrayList<CandleEntry>()
                        for (candle in candles) {
                            val open = candle.o
                            val close = candle.c
                            val high = candle.h
                            val low = candle.l
                            val time = candle.t
                            val timestamp = try {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(time)?.time ?: 0L
                            } catch (e: Exception) {
                                try {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(time)?.time ?: 0L
                                } catch (e2: Exception) {
                                    0L
                                }
                            }
                            val candleEntry = CandleEntry(timestamp.toFloat(), high.toFloat(), low.toFloat(), open.toFloat(), close.toFloat())
                            entries.add(candleEntry)
                        }
                        val candleDataSet = CandleDataSet(entries, "Price").apply {
                            setDrawValues(false)
                            colors = colors
                        }
                        val candleData = CandleData(candleDataSet)
                        setData(candleData)
                        
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                val date = Date(value.toLong())
                                val formatter = SimpleDateFormat("MMM dd", Locale.US)
                                return formatter.format(date)
                            }
                        }
                        
                        getAxisLeft().setDrawGridLines(false)
                        getAxisLeft().setDrawZeroLine(false)
                        getAxisRight().setDrawGridLines(false)
                        getAxisRight().setDrawZeroLine(false)
                        
                        animateX(1000)

                        // Render pattern overlay after data is set
                        val selectedPatternData = state.detections.find {
                            it.id == state.selectedPattern
                        }
                        selectedPatternData?.let { d ->
                            PatternOverlayRenderer(
                                chart,
                                chart.viewPortHandler
                            ).drawPatternOverlay(
                                PatternOverlay(
                                    anchors = d.anchors.map { 
                                        AnchorPoint(it.candle_index, it.price) 
                                    },
                                    neckline = d.neckline?.let { 
                                        NecklineInfo(
                                            it.candle_index.toFloat(),
                                            it.price.toFloat(),
                                            it.candle_index.toFloat()+3f,
                                            it.price.toFloat()
                                        )
                                    },
                                    targetPrice = d.target_price,
                                    stopPrice = d.stop_price,
                                    patternName = d.patternName,
                                    direction = d.direction,
                                    confidence = d.confidence,
                                    riskReward = d.risk_reward
                                )
                            )
                        }
                    }
                }
                
                chart
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Back")
                }
                
                IntervalDropdown(state.symbol, state.interval, state.range) { interval -> 
                    viewModel.updateInterval(interval)
                }
                
                RangeDropdown(state.symbol, state.range) { range -> 
                    viewModel.updateRange(range)
                }
                
                PatternDropdown(state.symbol) { pattern ->
                    viewModel.selectPattern(pattern)
                    viewModel.loadPatterns()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detection chips row (horizontal scroll)
            Text(
                text = "Pattern Detections",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.detections) { detection ->
                    DetectionChip(
                        detection = detection,
                        selected = state.selectedPattern == detection.id,
                        onClick = {
                            viewModel.selectPattern(detection.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trade-plan card
            if (state.tradePlan != null && state.tradePlanLoading.not()) {
                TradePlanCard(
                    plan = TradePlan(
                        entryPrice = state.tradePlan.entry_price,
                        entryDate = state.tradePlan.entry_date,
                        stopPrice = state.tradePlan.stop_price,
                        stopDate = state.tradePlan.stop_date,
                        targetPrice = state.tradePlan.target_price,
                        targetDate = state.tradePlan.target_date,
                        rr = state.tradePlan.risk_reward,
                        invalidation = state.tradePlan.invalidation,
                        positionSize = state.tradePlan.position_size?.toString() ?: "Set capital in Settings"
                    ),
                    onNavigate = {}
                )
            } else if (state.tradePlanLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Bottom sticky decision overlay / unavailable
            Spacer(modifier = Modifier.height(4.dp))
            val decVm = remember { TechnicalViewModel(NetworkModule.provideApi() as TpaApi) }
            val decision by decVm.decision.collectAsState(initial = null)
            LaunchedEffect(state.symbol) { decVm.load(state.symbol) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .glassSurface(elevation = 4f)
                    .padding(10.dp)
            ) {
                if (decision != null) {
                    DecisionCard(decision = decision!!)
                } else {
                    Text(
                        text = "Data unavailable — EOD sync pending",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
            }
        }
    }
}