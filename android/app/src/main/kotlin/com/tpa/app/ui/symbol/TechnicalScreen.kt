package com.tpa.app.ui.symbol

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.DecisionResponse
import com.tpa.app.network.MlBlock
import com.tpa.app.network.SrLevel
import com.tpa.app.network.TechnicalResponse
import com.tpa.app.ui.theme.glassSurface
import java.util.Locale

@Composable
fun TechnicalScreen(
    technical: TechnicalResponse?,
    decision: DecisionResponse?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Text("Technicals", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (technical == null) {
            Text("Technicals unavailable.", color = Color(0xFF94A3B8), fontSize = 12.sp)
        } else {
            // Gauges row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GaugeCard("RSI14", fmt(technical.rsi14, 1))
                GaugeCard("MACD h", fmt(technical.macdHistNorm, 2))
                GaugeCard("EMA20", fmt(technical.ema20, 1))
                GaugeCard("EMA50", fmt(technical.ema50, 1))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GaugeCard("ADX", fmt(technical.adx, 1))
                GaugeCard("ATR%", fmt(technical.atr14pct, 2))
                GaugeCard("BB W%", fmt(technical.bollingerWidthPct, 2))
                GaugeCard("Vol 5/20", fmt(technical.volRatio520, 2))
                GaugeCard("Del%", technical.deliveryPct5?.let { fmt(it, 1) } ?: "—")
            }

            Spacer(Modifier.height(8.dp))

            // Trend badge + structure chip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (technical.trendBadge.isNotBlank()) TrendBadge(technical.trendBadge)
                if (technical.structureChip.isNotBlank()) StructureChip(technical.structureChip)
            }

            Spacer(Modifier.height(8.dp))

            // S/R ladder (real support/resistance levels)
            val levels = buildList {
                addAll(technical.s_r.resistance.map { it to "R" })
                addAll(technical.s_r.support.map { it to "S" })
            }
            if (levels.isNotEmpty()) {
                Text("S / R Ladder", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    levels.take(6).forEach { (sr, kind) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().glassSurface().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(kind, color = if (kind == "R") Color(0xFFF87171) else Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(sr.price.format(2), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Str ${sr.strength} • ${sr.touches}t • ${sr.volume.toInt()}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Verdict card (bullish / bearish / neutral indicator counts)
            VerdictCard(technical.verdict)

            Spacer(Modifier.height(8.dp))

            // Candle chips (real per-candle patterns)
            if (technical.candleChips.isNotEmpty()) {
                Text("Candle Chips", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(technical.candleChips) { chip ->
                        Chip(label = chip.pattern, value = chip.strength.toString(), indicator = chip.date)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Decision card + ML outlook
        if (decision != null) {
            DecisionCard(decision)
            Spacer(Modifier.height(8.dp))
            MLOutlookCard(decision.components.ml)
        }
    }
}

private fun fmt(value: Double, decimals: Int): String = String.format(Locale.US, "%.${decimals}f", value)

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
private fun TrendBadge(trend: String) {
    val t = trend.lowercase(Locale.US)
    val color = when {
        t.contains("up") || t.contains("bull") -> Color(0xFF34D399)
        t.contains("down") || t.contains("bear") -> Color(0xFFF87171)
        else -> Color(0xFFFBBF24)
    }
    Row(
        modifier = Modifier.glassSurface().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text(trend, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StructureChip(structure: String) {
    val color = when (structure) {
        "UP", "CHOCH_UP", "MSS_UP" -> Color(0xFF34D399)
        "DOWN", "CHOCH_DOWN", "MSS_DOWN" -> Color(0xFFF87171)
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
        Text("Indicator Verdict", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VerdictItem("Bull", verdict.bullish.toString(), Color(0xFF34D399))
            VerdictItem("Bear", verdict.bearish.toString(), Color(0xFFF87171))
            VerdictItem("Neutral", verdict.neutral.toString(), Color(0xFFFBBF24))
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
private fun MLOutlookCard(ml: MlBlock) {
    Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("ML Outlook", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE2E8F0))
        MlRow("H1", ml.h1.up, ml.h1.down, ml.h1.sideways)
        MlRow("H2", ml.h2.up, ml.h2.down, ml.h2.sideways)
        MlRow("H5", ml.h5.up, ml.h5.down, ml.h5.sideways)
    }
}

@Composable
private fun MlRow(label: String, up: Double, down: Double, sideways: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(24.dp))
        Text("↑ ${fmt(up * 100, 0)}%", color = Color(0xFF34D399), fontSize = 12.sp)
        Text("↓ ${fmt(down * 100, 0)}%", color = Color(0xFFF87171), fontSize = 12.sp)
        Text("↔ ${fmt(sideways * 100, 0)}%", color = Color(0xFF94A3B8), fontSize = 12.sp)
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

private fun Double.format(d: Int) = String.format(Locale.US, "%.${d}f", this)
