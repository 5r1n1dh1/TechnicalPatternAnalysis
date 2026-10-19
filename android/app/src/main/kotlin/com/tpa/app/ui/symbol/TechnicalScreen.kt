package com.tpa.app.ui.symbol
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.TechnicalResponse
import com.tpa.app.network.DecisionResponse
import com.tpa.app.ui.theme.glassSurface

@Composable
fun TechnicalScreen(
    technical: TechnicalResponse?,
    decision: DecisionResponse?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Text("Technicals", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // Gauges row
        if (technical != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GaugeCard("RSI", "${String.format("%.1f", technical.rsi)}")
                GaugeCard("MACD", "${String.format("%.2f", technical.macd)}")
                GaugeCard("EMA20", "${String.format("%.1f", technical.ema20)}")
                GaugeCard("EMA50", "${String.format("%.1f", technical.ema50)}")
                GaugeCard("EMA200", "${String.format("%.1f", technical.ema200)}")
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GaugeCard("ADX", "${String.format("%.1f", technical.adx)}")
                GaugeCard("+DI/−DI", "${String.format("%.0f", technical.plus_di)} / ${String.format("%.0f", technical.minus_di)}")
                GaugeCard("ATR%", "${String.format("%.2f", technical.atr_percent)}%")
                GaugeCard("BB W", "${String.format("%.2f", technical.bb_width)}")
                GaugeCard("Del%", "${String.format("%.1f", technical.delivery_percent)}%")
                GaugeCard("RVOL", "${String.format("%.2f", technical.rvol)}")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Structure chip
        if (technical != null) {
            StructureChip(technical.structure)
        }

        Spacer(Modifier.height(8.dp))

        // S/R ladder
        if (technical != null && technical.sr_levels.isNotEmpty()) {
            Text("S / R Ladder", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                technical.sr_levels.take(4).forEach { sr ->
                    Row(
                        modifier = Modifier.fillMaxWidth().glassSurface().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sr.level + "  " + sr.type, color = Color.White, fontSize = 12.sp)
                        Text(sr.price.format(2), color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Str ${sr.strength} • ${sr.touches}t • ${sr.volume.toInt()}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Verdict card
        if (technical != null) {
            VerdictCard(technical.verdict)
        }

        Spacer(Modifier.height(8.dp))

        // ML Outlook (reused)
        if (technical != null) {
            MLOutlookCard(technical.outlook)
        }

        Spacer(Modifier.height(8.dp))

        // Decision card
        if (decision != null) {
            DecisionCard(decision)
        }

        // Candle chips
        if (technical != null && technical.candle_chips.isNotEmpty()) {
            Text("Candle Chips", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(technical.candle_chips) { chip ->
                    Chip(label = chip.label, value = chip.value.toString(), indicator = chip.indicator)
                }
            }
        }
    }
}

@Composable
private fun GaugeCard(title: String, value: String) {
    Column(
        modifier = Modifier.glassSurface().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Text(value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun StructureChip(structure: String) {
    val color = when (structure) {
        "UP" -> Color(0xFF34D399)
        "DOWN" -> Color(0xFFF87171)
        else -> Color(0xFFFBBF24)
    }
    Row(
        modifier = Modifier.glassSurface().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text(structure, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VerdictCard(verdict: com.tpa.app.network.Verdict) {
    Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp)) {
        Text("Verdict", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VerdictItem("B", verdict.bullish.toString(), Color(0xFF34D399))
            VerdictItem("Bear", verdict.bearish.toString(), Color(0xFFF87171))
            VerdictItem("Neu", verdict.neutral.toString(), Color(0xFFFBBF24))
            VerdictItem("Str", verdict.strength.toString(), Color(0xFFE2E8F0))
        }
    }
}

@Composable
private fun VerdictItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun MLOutlookCard(outlook: com.tpa.app.network.MLOutlook) {
    Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp)) {
        Text("ML Outlook", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
        Text(outlook.label + " • " + outlook.confidence.format(0) + "%", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DecisionCard(decision: DecisionResponse) {
    Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Decision", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
            Text(decision.verdict, color = when (decision.verdict) {
                "INVEST" -> Color(0xFF34D399)
                "SHORT" -> Color(0xFFF87171)
                else -> Color(0xFFFBBF24)
            }, fontWeight = FontWeight.Bold)
        }
        Text("Confidence: ${decision.confidence.format(0)}%", color = Color.White)
        if (decision.component_scores.isNotEmpty()) {
            Text("Components", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                decision.component_scores.take(4).forEach { s ->
                    Column {
                        Text(s.component, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(s.score.format(1), fontWeight = FontWeight.Bold, color = Color(0xFF34D399), fontSize = 12.sp)
                    }
                }
            }
        }
        if (decision.failing_rubric.isNotEmpty()) {
            Text("Failing rubric: ${decision.failing_rubric.joinToString()}", color = Color(0xFFF87171), fontSize = 11.sp)
        }
        if (decision.red_flags.isNotEmpty()) {
            Text("Red flags", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF87171))
            decision.red_flags.take(3).forEach { f ->
                Text("• ${f.title}: ${f.detail}", fontSize = 11.sp, color = Color(0xFFE2E8F0))
            }
        }
        if (decision.reasons.isNotEmpty()) {
            Text("Reasons: ${decision.reasons.joinToString("; ")}", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun Chip(label: String, value: String, indicator: String) {
    Column(modifier = Modifier.glassSurface().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8))
        Text(value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Text(indicator, fontSize = 9.sp, color = Color(0xFF34D399))
    }
}

private fun Double.format(d: Int) = String.format("%.${d}f", this)
