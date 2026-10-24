package com.tpa.app.ui.symbol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.DecisionResponse
import com.tpa.app.network.ComponentScore
import com.tpa.app.network.RedFlag
import com.tpa.app.ui.theme.glassSurface

@Composable
fun DecisionCard(decision: DecisionResponse, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Verdict badge + confidence
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VERDICT",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )
            val verdictColor = when (decision.verdict) {
                "INVEST" -> Color(0xFF34D399)
                "SHORT" -> Color(0xFFF87171)
                "WAIT" -> Color(0xFFFBBF24)
                else -> Color(0xFF94A3B8)
            }
            Badge(
                containerColor = verdictColor.copy(alpha = 0.15f),
                contentColor = verdictColor
            ) {
                Text(
                    text = decision.verdict,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Text(
            text = "Confidence ${"%.0f".format(decision.confidence * 100)}%",
            color = Color.White,
            fontSize = 12.sp
        )

        // Component scores row
        val componentScores = decision.resolvedComponentScores
        if (componentScores.isNotEmpty()) {
            Text(
                text = "Component Scores",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                componentScores.take(5).forEach { score: ComponentScore ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.component,
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = score.score.toInt().toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Reasons — always from server
        if (decision.reasons.isNotEmpty()) {
            Text(
                text = "Reasons",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(decision.reasons) { reason ->
                    Text(
                        text = "• $reason",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // Red flags card
        val redFlags = decision.resolvedRedFlags
        if (redFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Red Flags",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF87171)
            )
            redFlags.forEach { flag: RedFlag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (flag.severity) {
                                    "CRITICAL" -> Color(0xFFFF4444)
                                    "HIGH" -> Color(0xFFF87171)
                                    else -> Color(0xFFFBBF24)
                                },
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = flag.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = flag.detail,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
