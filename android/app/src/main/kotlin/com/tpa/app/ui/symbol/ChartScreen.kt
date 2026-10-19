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
import com.tpa.app.ui.chart.ChartState
import com.tpa.app.ui.chart.ChartViewModel
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.tpa.app.R
import com.tpa.app.ui.chart.IntervalDropdown
import com.tpa.app.ui.chart.RangeDropdown
import com.tpa.app.ui.chart.PatternDropdown
import com.tpa.app.ui.chart.ChartState
import com.tpa.app.ui.chart.ChartViewModel
import com.tpa.app.network.TpaApi
import com.tpa.app.network.Candle
import com.tpa.app.network.Interval
import com.tpa.app.network.Range
import com.tpa.app.ui.chart.PatternOverlayRenderer
import com.tpa.app.ui.chart.DetectionChip
import com.tpa.app.ui.chart.PatternOverlay
import com.tpa.app.ui.chart.AnchorPoint
import com.tpa.app.ui.chart.NecklineInfo

@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.state.value
    
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
                    isHighlightPerTouchEnabled = true
                    
                    // Configure candlestick chart
                    val colors = ColorTemplate.getColors(
                        Color(0xFF34D399),  // green up
                        Color(0xFFFF4444)    // red down
                    )
                    
                    val candleData = state.candles?.let { candles ->
                        val data = ArrayList<CandleData>()
                        
                        for (candle in candles) {
                            val open = candle.open.toDouble()
                            val close = candle.close.toDouble()
                            val high = candle.high.toDouble()
                            val low = candle.low.toDouble()
                            val time = candle.time
                        
                            val candleEntry = CandleEntry(
                                time.toFloat(),
                                open,
                                high,
                                low,
                                close
                            )
                            
                            data.add(candleEntry)
                        }
                        
                        CandleData(data, colors)
                    }
                    
                    if (data != null) {
                        candleData?.setDrawValues(false)
                        candleData?.setDrawValues(true)
                        candleData?.setValueTextSize(9f)
                        candleData?.setValueTextColor(Color.White)
                        
                        val xAxis = xAxis
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float, axis: XAxis?): String {
                                val date = Date(value.toLong())
                                val formatter = SimpleDateFormat("MMM dd", Locale.US)
                                return formatter.format(date)
                            }
                        }
                        
                        val leftAxis = getAxisLeft()
                        leftAxis.setDrawGridLines(false)
                        leftAxis.setDrawZeroLine(false)
                        
                        val rightAxis = getAxisRight()
                        rightAxis.setDrawGridLines(false)
                        rightAxis.setDrawZeroLine(false)
                        
                        data.setValueDrawOrder(ValueDrawOrder.SAVE_AND_REDRAW)
                        data.setValueTextSize(9f)
                        data.setValueTextColor(Color.White)
                        
                        setData(data)
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
                
                Button(
                    onClick = { IntervalDropdown(state.symbol, state.interval, state.range) { interval -> 
                        viewModel.updateInterval(interval)
                    } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Interval: ${state.interval}")
                }
                
                Button(
                    onClick = { RangeDropdown(state.symbol, state.range) { range -> 
                        viewModel.updateRange(range)
                    } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Range: ${state.range}")
                }
                
                Button(
                    onClick = { PatternDropdown(state.symbol) { pattern ->
                        viewModel.selectPattern(pattern)
                        viewModel.loadPatterns()
                    } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Patterns")
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

        }
    }
}