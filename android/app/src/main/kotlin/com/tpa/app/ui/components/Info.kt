package com.tpa.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class InfoTopic(val title: String, val body: String)

/** Plain-language explanations keyed by short identifiers used in the UI. */
object Glossary {
    val topics: Map<String, InfoTopic> = mapOf(
        "search" to InfoTopic("Search", "Type a company name or NSE symbol. Results are ranked by symbol prefix, then name prefix, then a fuzzy match. Tap a result to open its chart."),
        "quick" to InfoTopic("Quick look", "One-tap shortcuts to commonly viewed NSE symbols. Useful before you have saved a watchlist."),
        "wishlist" to InfoTopic("Wishlist", "Symbols saved with the star on a detail page. Cards open the same chart; the newest saved symbol appears first."),
        "bell" to InfoTopic("Notifications", "When on, a background check runs about every 60 minutes and posts a trend-change notification for saved symbols. Android notification permission is required."),
        "chart" to InfoTopic("Chart", "Candlesticks show open, high, low and close. Volume bars are scaled into the lower part of the price range so both share one axis."),
        "eod" to InfoTopic("EOD price", "End-of-day close from the latest candle returned by the server. Daily data is official end-of-day data, not a live tick."),
        "trend" to InfoTopic("Trend", "Server label (uptrend / downtrend / sideways) plus strength 0-100 from EMA and ADX rules. Higher strength means the direction is more consistent."),
        "sector" to InfoTopic("Sector", "Industry group for the symbol, resolved from the NSE symbol master."),
        "range" to InfoTopic("Range", "How much history to show: 1 day up to 10 years. Intraday ranges run from 1D to 1M, and only pairs the server can serve are offered."),
        "interval" to InfoTopic("Interval", "Candle size: 5m, 15m, 30m and 1h intraday, then 1d, 1w and 1M. Intraday candles use delayed Yahoo/Groww data; daily candles use the end-of-day dataset."),
        "detection" to InfoTopic("Pattern detections", "Shapes found by the server's pattern engine on the current interval and range. Changing either value re-runs detection."),
        "confidence" to InfoTopic("Confidence", "Pattern engine score 0-100. It reflects how closely the pivots match the textbook shape; it is not a probability of profit."),
        "rr" to InfoTopic("Risk : reward", "Distance to target divided by distance to stop. A value of 3.0 means the plan aims to make three rupees for every rupee risked."),
        "entry" to InfoTopic("Entry", "Price area where the pattern would trigger. The trade plan is educational, not an order."),
        "stop" to InfoTopic("Stop", "Price that invalidates the pattern. Risk is measured from entry to stop."),
        "target" to InfoTopic("Target", "Measured-move price for the pattern. Treat it as a reference, not a guarantee."),
        "invalidation" to InfoTopic("Invalidation", "The condition that cancels the setup, for example a close back above the neckline."),
        "position" to InfoTopic("Position size", "Number of shares implied by the server's risk settings. Educational only."),
        "rsi" to InfoTopic("RSI 14", "Relative Strength Index over 14 periods. Above 70 is often called overbought, below 30 oversold; strong trends can stay extended."),
        "macd" to InfoTopic("MACD histogram", "Normalised difference between the 12/26 EMA and the 9-period signal line. Positive bars mean short-term momentum is above the signal."),
        "ema" to InfoTopic("EMA 20 / 50", "Exponential moving averages. Price above the 20 EMA is short-term strong; 20 above 50 is a bullish alignment."),
        "adx" to InfoTopic("ADX", "Trend-strength indicator. Below 20 is weak or choppy, above 25 is a trending market; it says nothing about direction."),
        "atr" to InfoTopic("ATR %", "Average True Range as a percentage of price over 14 periods. Higher values mean wider daily swings and wider stops."),
        "bb" to InfoTopic("Bollinger width %", "Distance between the upper and lower Bollinger bands as a percentage of price. Low width is a squeeze; high width is expansion."),
        "volratio" to InfoTopic("Volume 5/20", "Five-day average volume divided by the twenty-day average. Above 1 means volume is picking up."),
        "delivery" to InfoTopic("Delivery %", "Share of traded volume that resulted in delivery. Higher delivery often means stronger conviction."),
        "sr" to InfoTopic("S/R ladder", "Support and resistance prices with strength, touch count and volume. More touches and higher strength make a level more significant."),
        "candlechip" to InfoTopic("Candle chips", "Recent candlestick patterns (doji, engulfing, and similar) with a strength score."),
        "verdict" to InfoTopic("Verdict", "Bullish / bearish / neutral vote across the technical checks. The decision card combines it with valuation and pattern state."),
        "components" to InfoTopic("Score components", "Rule-based score for each part of the decision. Confidence aggregates them; red flags are hard or soft warnings."),
        "mcap" to InfoTopic("Market cap", "Share price multiplied by shares outstanding. Shown in crore; one crore is ten million."),
        "pe" to InfoTopic("P/E (TTM)", "Price divided by trailing twelve-month earnings per share. Higher means the market pays more per rupee of current profit."),
        "pb" to InfoTopic("P/B", "Price divided by book value per share. Most useful for banks and asset-heavy companies."),
        "ev" to InfoTopic("EV/EBITDA", "Enterprise value divided by EBITDA. A valuation multiple that ignores capital structure."),
        "divyield" to InfoTopic("Dividend yield", "Annual dividend per share divided by price, in percent."),
        "eps" to InfoTopic("EPS", "Earnings per share: profit attributable to shareholders divided by shares outstanding."),
        "bv" to InfoTopic("Book value", "Accounting net worth per share: equity divided by shares outstanding."),
        "roa" to InfoTopic("ROA", "Net profit divided by total assets; how efficiently assets generate profit."),
        "roce" to InfoTopic("ROCE", "EBIT divided by capital employed; return on all long-term capital before financing mix."),
        "roic" to InfoTopic("ROIC", "NOPAT divided by invested capital; return on the money actually invested in the business."),
        "margins" to InfoTopic("Margins", "Gross, net and EBITDA margins as a percentage of revenue. Higher margins leave more cushion for costs."),
        "shareholding" to InfoTopic("Shareholding", "Promoter, FII, DII and public stakes. Large promoter holdings can mean control; falling promoter stakes can be a flag."),
        "dcf" to InfoTopic("DCF estimate", "Discounted cash flow value using the server's growth and WACC assumptions. It is sensitive to those inputs."),
        "redflags" to InfoTopic("Red flags", "Hard flags break a rule (for example leverage); soft flags are warnings. They reduce decision confidence."),
        "fundamentals" to InfoTopic("Fundamentals", "Reported and computed financials. Values tagged reported come from filings; computed values are derived by the server."),
        "technicals" to InfoTopic("Technicals", "Indicator snapshot for the latest bar, computed from the same candle series as the chart."),
        "overlay" to InfoTopic("Pattern overlay", "Anchor dots trace the pattern. The neckline, target and stop are labeled horizontal levels; tap another detection to compare."),
        "pivots" to InfoTopic("Pivot points", "Swing highs (amber) and lows (blue) accepted by the pattern engine. Tap a detection to see its total, high/low counts, and each pivot's time and price. Daily candles are end-of-day; intraday candles are delayed.")
    )

    fun of(vararg keys: String): List<InfoTopic> =
        keys.mapNotNull { topics[it] }
}

/** Small info affordance. Opens a scrollable dialog that explains [keys]. */
@Composable
fun InfoButton(
    title: String,
    keys: List<String>,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val topics = remember(keys) { Glossary.of(*keys.toTypedArray()) }

    IconButton(onClick = { open = true }, modifier = modifier.size(32.dp)) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "More info: $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topics) { topic ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = topic.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
fun GuideButton(
    title: String,
    vararg keys: String,
    modifier: Modifier = Modifier
) = InfoButton(title = title, keys = keys.toList(), modifier = modifier)
