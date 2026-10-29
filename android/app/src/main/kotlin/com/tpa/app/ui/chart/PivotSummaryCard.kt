package com.tpa.app.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.Candle
import com.tpa.app.network.Detection
import com.tpa.app.ui.components.GuideButton
import com.tpa.app.ui.theme.glassSurface
import java.text.SimpleDateFormat
import java.util.Locale

private val pivotTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
private val clockFormat = SimpleDateFormat("HH:mm", Locale.US)
private val intradayFormat = SimpleDateFormat("dd MMM HH:mm", Locale.US)
private val dayFormat = SimpleDateFormat("dd MMM", Locale.US)

/**
 * Counts and rows for the pivot points attached to the selected detection.
 * Bars are matched by server index so intraday timestamps cannot drift.
 */
@Composable
fun PivotSummaryCard(
    detection: Detection,
    candles: List<Candle>,
    interval: String,
    range: String,
    modifier: Modifier = Modifier
) {
    val pivots = detection.pivots.sortedBy { it.idx }
    val highs = pivots.count { it.kind.equals("HIGH", ignoreCase = true) }
    val lows = pivots.size - highs

    Column(
        modifier = modifier.fillMaxWidth().glassSurface().padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Pivot points",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = pivots.size.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.weight(1f))
            GuideButton("Pivot points", "pivots")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (pivots.isEmpty()) {
                "No pivot points attached to this detection."
            } else {
                "$highs high · $lows low"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (pivots.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            pivots.forEachIndexed { index, pivot ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                val isHigh = pivot.kind.equals("HIGH", ignoreCase = true)
                val marker = if (isHigh) PivotMarker.highColor else PivotMarker.lowColor
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(7.dp).background(marker, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = pivotTime(pivot.idx, pivot.t, candles, interval, range),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isHigh) "HIGH" else "LOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = marker
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f", pivot.price),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun pivotTime(
    idx: Int,
    raw: String,
    candles: List<Candle>,
    interval: String,
    range: String
): String {
    val stamp = candles.getOrNull(idx)?.t ?: raw
    val parsed = try {
        pivotTimeFormat.parse(stamp)
    } catch (e: Exception) {
        null
    } ?: return raw.replace('T', ' ').take(16)
    val formatter = when {
        range == "1D" -> clockFormat
        interval == "5m" || interval == "15m" || interval == "30m" || interval == "1h" -> intradayFormat
        else -> dayFormat
    }
    return formatter.format(parsed)
}
