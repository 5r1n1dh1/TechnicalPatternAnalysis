package com.tpa.app.ui.symbol

import androidx.compose.foundation.layout.*
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
                        // Handle pattern selection
                    } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Patterns")
                }
            }
        }
    }
}