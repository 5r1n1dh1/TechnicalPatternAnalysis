package com.tpa.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tpa.app.ui.theme.glassSurface
import com.tpa.app.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(vm: WatchlistViewModel) {
    val list = vm.items.collectAsState().value
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Watchlist empty", color = Color(0xFF94A3B8))
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().glassSurface().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.symbol, color = Color.White, fontWeight = FontWeight.Bold)
                TextButton(onClick = { vm.remove(entry.symbol) }) { Text("Remove") }
            }
        }
    }
}
