package com.tpa.app.ui.symbol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tpa.app.network.*
import com.tpa.app.ui.theme.*

@Composable
fun FundamentalsScreen(
    data: FundamentalsResponse?,
    modifier: Modifier = Modifier
) {
    if (data == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard(data.header) }
        item { ValuationCard(data.valuation) }
        item { ProfitabilityCard(data.profitability) }
        item { RatioGridCard(data.ratios) }
        item { QuarterlyTableCard(data.quarterlyResults) }
        item { AnnualStatementsCard(data.annualStatements) }
        item { RedFlagsCard(data.redFlags) }
        item { ShareholdingCard(data.shareholding) }
        item { CorporateActionsCard(data.corporateActions) }
        item { DcfCard(data.dcf) }
    }
}

@Composable
private fun HeaderCard(h: Header) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text(h.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.sector, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("MCap: ${String.format("%,.0f", h.mcap)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("52w: ${h.w52Range}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ValuationCard(v: Valuation) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Valuation", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric("PE", v.pe_ttm, "computed")
            Metric("PB", v.pb, "reported")
            Metric("EV/EBITDA", v.evToEbitda, "computed")
            Metric("Div Yield", v.dividendYield, "reported")
            Metric("EPS", v.eps, "computed")
            Metric("BV", v.bookValue, "reported")
        }
    }
}

@Composable
private fun ProfitabilityCard(p: Profitability) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Profitability", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric("ROA", p.roa, "computed")
            Metric("ROCE", p.roce, "computed")
            Metric("ROIC", p.roic, "computed")
            Metric("GM", p.grossMargin, "reported")
            Metric("NM", p.netMargin, "reported")
            Metric("EBITDA M", p.ebitdaMargin, "computed")
        }
    }
}

@Composable
private fun RatioGridCard(ratios: List<RatioItem>) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Ratio Grid", style = MaterialTheme.typography.titleMedium)
        val list = if (ratios.isNotEmpty()) ratios else listOf(
            RatioItem("Current Ratio", null, "missing"),
            RatioItem("Quick Ratio", null, "missing"),
            RatioItem("D/E", null, "missing"),
            RatioItem("Interest Cov", null, "missing"),
            RatioItem("Asset Turn", null, "missing"),
            RatioItem("DSO", null, "missing"),
            RatioItem("DPO", null, "missing"),
            RatioItem("CCC", null, "missing"),
            RatioItem("PE", null, "missing"),
            RatioItem("PB", null, "missing"),
            RatioItem("EV/EBITDA", null, "missing"),
            RatioItem("Div Yield", null, "missing"),
            RatioItem("PEG", null, "missing"),
            RatioItem("FCF Yield", null, "missing"),
            RatioItem("ROIC", null, "missing"),
            RatioItem("Op Margin", null, "missing"),
            RatioItem("Rev Growth", null, "missing"),
            RatioItem("EPS Growth", null, "missing"),
            RatioItem("FCF Growth", null, "missing"),
            RatioItem("Book/Share", null, "missing"),
            RatioItem("Cash/Share", null, "missing"),
            RatioItem("Total Debt", null, "missing"),
            RatioItem("Net Debt", null, "missing"),
            RatioItem("Equity", null, "missing"),
            RatioItem("Total Assets", null, "missing")
        )
        val chunked = list.chunked(3)
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = item.value?.let { String.format("%.2f", it) + " (${item.provenance})" } ?: "— (${item.provenance})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarterlyTableCard(results: List<QuarterlyResult>) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Quarterly Results (last 8q)", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Q", "Rev", "PAT", "EPS", "YoY Rev").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        results.take(8).forEach { r ->
            Row(modifier = Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.q, style = MaterialTheme.typography.bodySmall)
                Text(String.format("%,.0f", r.rev), style = MaterialTheme.typography.bodySmall)
                Text(String.format("%,.0f", r.pat), style = MaterialTheme.typography.bodySmall)
                Text(String.format("%.2f", r.eps), style = MaterialTheme.typography.bodySmall)
                Text(String.format("%.1f%%", r.yoyRev), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AnnualStatementsCard(s: AnnualStatements) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Annual Statements", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Collapse" else "Expand")
        }
        if (expanded) {
            Divider()
            Text("P&L — Rev: ${String.format("%,.0f", s.pnl.revenue)}, PAT: ${String.format("%,.0f", s.pnl.pat)}", style = MaterialTheme.typography.bodySmall)
            Text("Balance — Assets: ${String.format("%,.0f", s.balanceSheet.totalAssets)}, Debt: ${String.format("%,.0f", s.balanceSheet.totalDebt)}, Equity: ${String.format("%,.0f", s.balanceSheet.equity)}", style = MaterialTheme.typography.bodySmall)
            Text("Cash Flow — OCF: ${String.format("%,.0f", s.cashFlow.ocf)}, CapEx: ${String.format("%,.0f", s.cashFlow.capex)}, FCF: ${s.cashFlow.fcf?.let { String.format("%,.0f", it) } ?: "—"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RedFlagsCard(rf: RedFlags) {
    val flags = mutableListOf<String>()
    if (rf.earningsQuality) flags.add("earningsQuality (hard)")
    if (rf.leverage) flags.add("leverage (hard)")
    if (rf.growth) flags.add("growth (soft)")
    if (rf.management) flags.add("management (soft)")
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Red Flags", style = MaterialTheme.typography.titleMedium)
        if (flags.isEmpty()) {
            Text("None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            flags.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ShareholdingCard(s: Shareholding) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Shareholding", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricCol("Promoter", s.promoter)
            MetricCol("FII", s.fii)
            MetricCol("DII", s.dii)
            MetricCol("Public", s.public)
        }
    }
}

@Composable
private fun CorporateActionsCard(ca: List<CorporateAction>) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("Corporate Actions", style = MaterialTheme.typography.titleMedium)
        if (ca.isEmpty()) {
            Text("None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ca.forEach {
                Text("• ${it.type} (${it.date}): ${it.detail}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DcfCard(d: DcfEstimate) {
    Column(modifier = Modifier.glassCard().padding(12.dp)) {
        Text("DCF Estimate", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricCol("WACC", String.format("%.1f%%", d.wacc * 100))
            MetricCol("Term g", String.format("%.1f%%", d.terminalGrowth * 100))
            MetricCol("Value/Sh", d.equityValuePerShare?.let { "%.0f".format(it) } ?: "—")
            MetricCol("Upside", d.upsidePercent?.let { "%.1f%%".format(it) } ?: "—")
        }
    }
}

@Composable
private fun Metric(label: String, value: Double?, provenance: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value?.let { "${String.format("%.2f", it)} (${provenance})" } ?: "— (${provenance})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetricCol(label: String, valueText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
