package com.tpa.app.ui.symbol

import androidx.compose.foundation.background
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

data class TradePlan(
    val entryPrice: Double,
    val entryDate: String,
    val stopPrice: Double,
    val stopDate: String,
    val targetPrice: Double,
    val targetDate: String,
    val rr: Double,
    val invalidation: String,
    val positionSize: String = "Not set"
)

@Composable
fun TradePlanCard(
    plan: TradePlan,
    onNavigate: () -> Unit
) {
    val accentColor = if (plan.rr >= 3) Color(0xFF34D399) else Color(0xFFFF4444)
    val backgroundColor = Color(0x0BFFFFFF)
    val borderColor = Color(0x1FCCCCCC)
    val textPrimary = Color.White
    val textSecondary = Color(0xBBFFFFFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry (${plan.entryDate})",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Price: ${plan.entryPrice}",
                    color = textSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stop (${plan.stopDate})",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Price: ${plan.stopPrice}",
                    color = textSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target (${plan.targetDate})",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Price: ${plan.targetPrice}",
                    color = textSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFFFFF))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "R:R",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentColor)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${plan.rr.toInt()}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Invalidation",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = plan.invalidation,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Position Size",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = plan.positionSize,
                    color = textSecondary
                )
            }
        }
    }
}