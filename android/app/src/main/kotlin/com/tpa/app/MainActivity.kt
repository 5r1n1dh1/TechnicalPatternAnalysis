package com.tpa.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tpa.app.ui.symbol.SymbolScreen
import com.tpa.app.ui.symbol.ChartScreen
import com.tpa.app.ui.symbol.ChartViewModel
import com.tpa.app.network.NetworkModule
import com.tpa.app.ui.chart.IntervalDropdown
import com.tpa.app.ui.chart.RangeDropdown
import com.tpa.app.ui.chart.PatternDropdown

@Composable
fun Greeting(name: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ChartScreen(
            viewModel = ChartViewModel(NetworkModule.provideApi()),
            onBack = onBack
        )
    }
}

@Composable
fun App() {
    MaterialTheme {
        Greeting("AAPL", onBack = { /* Handle back */ })
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        App()
    }
}