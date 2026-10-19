package com.tpa.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.DecisionResponse
import com.tpa.app.network.TpaApi
import com.tpa.app.network.SearchResult
import com.tpa.app.ui.theme.glassSurface
import com.tpa.app.ui.symbol.DecisionCard
import com.tpa.app.network.NetworkModule

// Minimal search result model (not in API contract; local for UI)
data class SearchResult(
    val symbol: String,
    val name: String,
    val sector: String
)

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val api = NetworkModule.provideApi()
    var query by remember { mutableStateOf("RELIANCE") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var selected by remember { mutableStateOf<SearchResult?>(null) }
    var decision by remember { mutableStateOf<DecisionResponse?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search symbol") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            results = listOf(
                SearchResult("RELIANCE", "Reliance Industries", "Energy"),
                SearchResult("HDFCBANK", "HDFC Bank", "Financials")
            )
            selected = null
            decision = null
        }) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (results.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { result ->
                    Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(result.symbol, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(result.name, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(result.sector, fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                            if (selected?.symbol == result.symbol && decision != null) {
                                Badge(containerColor = Color(0xFF34D399).copy(alpha = 0.2f), contentColor = Color(0xFF34D399)) {
                                    Text(decision!!.verdict)
                                }
                            } else {
                                Badge(containerColor = Color(0xFF94A3B8).copy(alpha = 0.2f), contentColor = Color(0xFF94A3B8)) {
                                    Text("Lazy")
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // Quick-decision chip row — lazy load only on selection
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("INVEST", "SHORT", "WAIT", "WATCH").forEach { chip ->
                                AssistChip(
                                    onClick = {
                                        selected = result
                                        loading = true
                                        // Lazy load decision only on tap
                                        try {
                                            // In real code: api.getDecision(result.symbol)
                                            // Mock loaded decision for demo; server delivers verdict + reasons
                                            decision = DecisionResponse(
                                                symbol = result.symbol,
                                                verdict = chip,
                                                confidence = 0.78,
                                                component_scores = listOf(
                                                    com.tpa.app.network.ComponentScore("Fundamental", 72.0),
                                                    com.tpa.app.network.ComponentScore("Technical", 65.0),
                                                    com.tpa.app.network.ComponentScore("Valuation", 60.0),
                                                    com.tpa.app.network.ComponentScore("Pattern", 55.0),
                                                    com.tpa.app.network.ComponentScore("ML", 58.0)
                                                ),
                                                failing_rubric = emptyList(),
                                                red_flags = emptyList(),
                                                reasons = listOf("Server verdict $chip with 78% confidence.")
                                            )
                                        } catch (_: Exception) { }
                                        loading = false
                                    },
                                    label = { Text(chip) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
