package com.tpa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.tpa.app.network.NetworkModule
import com.tpa.app.ui.symbol.ChartScreen
import com.tpa.app.ui.symbol.ChartViewModel
import com.tpa.app.ui.symbol.FundamentalsScreen
import com.tpa.app.ui.symbol.FundamentalsViewModel
import com.tpa.app.ui.symbol.TechnicalScreen
import com.tpa.app.ui.symbol.TechnicalViewModel
import com.tpa.app.ui.theme.TechnicalPatternAnalysisTheme

private const val DEFAULT_SYMBOL = "RELIANCE"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TechnicalPatternAnalysisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var selectedTab by remember { mutableStateOf(1) } // 0=Fundamentals, 1=Charts+Decision, 2=Technicals
                    Column(modifier = Modifier.fillMaxSize()) {
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
                        when (selectedTab) {
                            0 -> FundamentalsTab()
                            1 -> ChartScreen(
                                viewModel = ChartViewModel(NetworkModule.provideApi()),
                                onBack = {}
                            )

                            2 -> TechnicalsTab()
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FundamentalsTab() {
    val api = NetworkModule.provideApi()
    val viewModel = remember(api) { FundamentalsViewModel(api, DEFAULT_SYMBOL) }
    val state by viewModel.state.collectAsState()

    when {
        state.loading && state.data == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        state.data == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                state.error?.let { "Fundamentals unavailable: $it" } ?: "Fundamentals unavailable",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        else -> FundamentalsScreen(data = state.data)
    }
}

@Composable
private fun TechnicalsTab() {
    val api = NetworkModule.provideApi()
    val viewModel = remember(api) { TechnicalViewModel(api) }
    val technical by viewModel.technical.collectAsState()
    val decision by viewModel.decision.collectAsState()

    LaunchedEffect(Unit) { viewModel.load(DEFAULT_SYMBOL) }

    TechnicalScreen(
        technical = technical,
        decision = decision,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    )
}
