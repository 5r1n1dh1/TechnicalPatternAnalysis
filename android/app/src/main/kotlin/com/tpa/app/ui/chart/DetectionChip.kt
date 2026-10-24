package com.tpa.app.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.Detection

@Composable
fun DetectionChip(
    detection: Detection,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val confidenceColor = if (detection.confidence >= 70) Color(0xFF34D399) else {
        if (detection.confidence >= 45) Color(0xFFFBBF24) else Color(0xFFF87171)
    }
    val rr = detection.risk_reward
    val rrColor = when {
        rr == null -> Color(0xFF94A3B8)
        rr >= 3 -> Color(0xFF34D399)
        rr > 2 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    val isBull = detection.direction.lowercase().startsWith("bull")
    val directionIcon = if (isBull) "\uD83D\uDC8A" else "\uD83D\uDC89"
    val accentColor = if (isBull) Color(0xFF34D399) else Color(0xFFFF4444)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(width = 2.dp, color = accentColor, shape = RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x0BFFFFFF))
                .border(
                    width = 1.dp,
                    color = Color(0x1FCCCCCC),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: pattern name + icon
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = detection.patternName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = directionIcon,
                        fontSize = 14.sp,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ID: ${detection.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xBBFFFFFF)
                )
            }

            // Middle: confidence badge
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${detection.confidence.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 9.sp
                )
            }

            // Right: R:R badge
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rr?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = rrColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
