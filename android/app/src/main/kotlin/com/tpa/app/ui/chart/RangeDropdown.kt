package com.tpa.app.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tpa.app.R

@Composable
fun RangeDropdown(
    symbol: String,
    range: Range,
    onRangeChange: (Range) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val validRanges = getValidRanges()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Range: ${range.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_menu_down),
                    contentDescription = "Menu",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        if (expanded) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                validRanges.forEach { rangeOption ->
                    DropdownMenuItem(
                        text = { Text(rangeOption.value) },
                        onClick = {
                            onRangeChange(rangeOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    
    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Range",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                validRanges.forEach { rangeOption ->
                    Button(
                        onClick = {
                            onRangeChange(rangeOption)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(rangeOption.value)
                    }
                }
            }
        }
    }
}

private fun getValidRanges(): List<Range> {
    return listOf(
        Range("1D"),
        Range("5D"),
        Range("1M"),
        Range("3M"),
        Range("6M"),
        Range("1Y"),
        Range("2Y"),
        Range("5Y"),
        Range("10Y")
    )
}