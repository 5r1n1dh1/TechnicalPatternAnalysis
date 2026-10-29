package com.tpa.app.ui.chart

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.Detection
import com.tpa.app.ui.theme.LocalTpaTokens
import java.util.Locale

@Composable
fun DetectionChip(
    detection: Detection,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val tokens = LocalTpaTokens.current
    val isBull = detection.direction.lowercase().startsWith("bull")
    val accent = if (isBull) tokens.bullish else tokens.bearish
    val confidenceColor = when {
        detection.confidence >= 70 -> tokens.bullish
        detection.confidence >= 45 -> tokens.warning
        else -> tokens.bearish
    }
    val rr = detection.risk_reward

    Card(
        modifier = Modifier
            .widthIn(min = 170.dp, max = 230.dp)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(18.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tokens.glassFill)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = detection.patternName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBull) "▲" else "▼",
                    color = accent,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${detection.confidence.toInt()}% confidence",
                    color = confidenceColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = rr?.let { String.format(Locale.US, "R:R %.1f", it) } ?: "R:R —",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
