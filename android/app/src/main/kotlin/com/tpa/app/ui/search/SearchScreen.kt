package com.tpa.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.DecisionResponse
import com.tpa.app.network.NetworkModule
import com.tpa.app.network.SearchResult
import com.tpa.app.ui.symbol.DecisionCard
import com.tpa.app.ui.theme.glassSurface
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val api = NetworkModule.provideApi()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var selected by remember { mutableStateOf<SearchResult?>(null) }
    var decision by remember { mutableStateOf<DecisionResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search symbol") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            enabled = !loading,
            onClick = {
                loading = true
                error = null
                selected = null
                decision = null
                scope.launch {
                    try {
                        results = api.search(query.trim().ifBlank { null })
                    } catch (e: Exception) {
                        results = emptyList()
                        error = "Search failed: ${e.message}"
                    }
                    loading = false
                }
            }
        ) {
            Text(if (loading) "Searching…" else "Search")
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { result ->
                Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(result.symbol, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(result.name, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(result.sector, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        Button(onClick = {
                            selected = result
                            loading = true
                            decision = null
                            scope.launch {
                                try {
                                    decision = api.getDecision(result.symbol)
                                } catch (e: Exception) {
                                    error = "Decision failed: ${e.message}"
                                }
                                loading = false
                            }
                        }) {
                            Text("Decide")
                        }
                    }
                    if (selected?.symbol == result.symbol && decision != null) {
                        Spacer(Modifier.height(6.dp))
                        DecisionCard(decision!!)
                    }
                }
            }
        }
    }
}
