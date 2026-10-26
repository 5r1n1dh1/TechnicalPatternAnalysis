package com.tpa.app.ui.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tpa.app.data.wishlist.WishlistEntry
import com.tpa.app.ui.theme.LocalTpaTokens
import com.tpa.app.ui.theme.glassCard
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.tpa.app.notify.NotifChannels
import com.tpa.app.setupTrendWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WishlistScreen(viewModel: WishlistViewModel, onOpen: (String) -> Unit = {}) {
    val entries = viewModel.entries.collectAsState().value
    val ctx = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Wishlist", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            val prefs = ctx.getSharedPreferences("tpa_settings", android.content.Context.MODE_PRIVATE)
            var enabled by remember { mutableStateOf(prefs.getBoolean("notif_enabled", true)) }
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                prefs.edit().putBoolean("notif_enabled", it).apply()
                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        (ctx as? android.app.Activity)?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                    }
                    if (it) setupTrendWorker(ctx)
                } else {
                    androidx.work.WorkManager.getInstance(ctx).cancelUniqueWork("trend-check")
                }
            })
        }
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
                color = LocalTpaTokens.current.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
