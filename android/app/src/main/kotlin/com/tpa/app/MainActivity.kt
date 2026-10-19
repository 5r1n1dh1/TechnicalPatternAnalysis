package com.tpa.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tpa.app.ui.symbol.ChartScreen
import com.tpa.app.ui.symbol.ChartViewModel
import com.tpa.app.network.NetworkModule

@Composable
fun MainActivity() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var selectedTab by remember { mutableStateOf(1) } // 0=Fundamentals, 1=Charts+Decision, 2=Technicals
            Column(modifier = Modifier.fillMaxSize()) {
                // Navigation tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Fundamentals", "Charts", "Technicals").forEachIndexed { index, label ->
                        TextButton(
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Content
                when (selectedTab) {
                    0 -> FundamentalsPlaceholder()
                    1 -> ChartScreen(
                        viewModel = ChartViewModel(NetworkModule.provideApi()),
                        onBack = {}
                    )
                    2 -> TechnicalsPlaceholder()
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun FundamentalsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Fundamentals — M6")
    }
}

@Composable
private fun TechnicalsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Technicals — M4")
    }
}
