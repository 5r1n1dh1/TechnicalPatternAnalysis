package com.tpa.app.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.R
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.ChartViewModel

@Composable
fun PatternDropdown(
    symbol: String,
    onPatternChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
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
                text = "Patterns",
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
                val patterns = listOf(
                    "all",
                    "head_and_shoulders",
                    "cup_and_handle",
                    "double_triple",
                    "flag"
                )
                
                patterns.forEach { pattern ->
                    DropdownMenuItem(
                        text = { Text(pattern) },
                        onClick = {
                            onPatternChange(pattern)
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
                    text = "Select Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                patterns.forEach { pattern ->
                    Button(
                        onClick = {
                            onPatternChange(pattern)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(pattern)
                    }
                }
            }
        }
    }
}