package com.tpa.app.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tpa.app.network.Candle

/** T-47 volume sub-chart: bar representation of real candle `v` using theme/glass tokens. */
@Composable
fun VolumeBars(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    // Volume bars consume the same `v` value from the DTO used by the price chart;
    // color mapping uses Bullish/Bearish theme tokens (T-42).
    // Actual rendering delegated to MPAndroidChart BarData inside ChartScreen.kt.
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Volume",
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
