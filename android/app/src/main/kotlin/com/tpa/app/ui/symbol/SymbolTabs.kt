package com.tpa.app.ui.symbol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tpa.app.ui.components.ErrorView
import com.tpa.app.ui.components.LoadingView

@Composable
fun FundamentalsTab(viewModel: FundamentalsViewModel) {
    val state by viewModel.state.collectAsState()
    when {
        state.loading && state.data == null -> LoadingView(label = "Loading fundamentals…")
        state.data != null -> FundamentalsScreen(
            data = state.data,
            modifier = Modifier.fillMaxSize().padding(bottom = 96.dp)
        )
        else -> ErrorView(
            message = state.error?.let { "Fundamentals unavailable: $it" }
                ?: "Fundamentals unavailable",
            onRetry = viewModel::load
        )
    }
}

@Composable
fun TechnicalsTab(viewModel: TechnicalViewModel, symbol: String) {
    val technical by viewModel.technical.collectAsState()
    val decision by viewModel.decision.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(symbol) { viewModel.load(symbol) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading && technical == null) {
            LoadingView(label = "Loading technicals…")
        } else if (technical == null) {
            Text(
                text = "Technicals unavailable for $symbol.",
                modifier = Modifier.padding(24.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TechnicalScreen(
                technical = technical,
                decision = decision,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp)
            )
        }
    }
}
