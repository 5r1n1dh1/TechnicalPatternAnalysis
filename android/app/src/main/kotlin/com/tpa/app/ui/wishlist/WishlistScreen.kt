package com.tpa.app.ui.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tpa.app.data.wishlist.WishlistEntry
import com.tpa.app.ui.theme.glassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WishlistScreen(viewModel: WishlistViewModel, onOpen: (String) -> Unit = {}) {
    val entries = viewModel.entries.collectAsState().value
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Wishlist", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No symbols added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.symbol }) { e ->
                    GlassWishlistCard(e = e, onClick = { onOpen(e.symbol) })
                }
            }
        }
    }
}

@Composable
private fun GlassWishlistCard(e: WishlistEntry, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth().glassCard(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(e.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(e.addedAt)),
                color = MaterialTheme.colorScheme.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
