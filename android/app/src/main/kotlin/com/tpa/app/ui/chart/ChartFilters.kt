package com.tpa.app.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.R
import com.tpa.app.ui.components.GuideButton

/** Ranges the EOD dataset can serve. 1D is intraday-only and therefore absent. */
val ChartRanges = listOf("5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y")

/** Intervals backed by the baked EOD dataset. */
val ChartIntervals = listOf("1d", "1w", "1M")

fun allowedIntervals(range: String): List<String> = when (range) {
    "5D", "1M", "3M", "6M" -> listOf("1d")
    "1Y", "2Y", "5Y", "10Y" -> listOf("1d", "1w", "1M")
    else -> listOf("1d")
}

fun allowedRanges(interval: String): List<String> = when (interval) {
    "1w", "1M" -> listOf("1Y", "2Y", "5Y", "10Y")
    else -> ChartRanges
}

fun clampRangeFor(interval: String, range: String): String =
    if (range in allowedRanges(interval)) range else "1Y"

fun clampIntervalFor(range: String, interval: String): String =
    if (interval in allowedIntervals(range)) interval else "1d"

@Composable
fun ChartFilters(
    range: Range,
    interval: Interval,
    onRangeChange: (Range) -> Unit,
    onIntervalChange: (Interval) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterDropdown(
            label = "Range",
            value = range.value,
            options = allowedRanges(interval.value),
            modifier = Modifier.weight(1f)
        ) { onRangeChange(Range(it)) }

        FilterDropdown(
            label = "Interval",
            value = interval.value,
            options = allowedIntervals(range.value),
            modifier = Modifier.weight(1f)
        ) { onIntervalChange(Interval(it)) }

        GuideButton("Chart filters", "range", "interval", "detection")
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_down),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
