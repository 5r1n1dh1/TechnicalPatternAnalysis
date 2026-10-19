package com.tpa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tpa.app.network.NetworkModule
import com.tpa.app.ui.symbol.ChartScreen
import com.tpa.app.ui.theme.TechnicalPatternAnalysisTheme
import com.tpa.app.ui.symbol.ChartViewModel

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
    }
}

@Composable
private fun FundamentalsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Fundamentals — M6")
    }
}

@Composable
private fun TechnicalsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Technicals — M4")
    }
}