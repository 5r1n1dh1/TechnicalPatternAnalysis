package com.tpa.app.ui.wishlist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tpa.app.notify.notificationsEnabled
import com.tpa.app.notify.setNotificationsEnabled
import com.tpa.app.ui.components.EmptyView
import com.tpa.app.ui.components.SymbolCard

@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    onOpenSymbol: (String) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(notificationsEnabled(context)) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wishlist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (entries.isEmpty()) "Nothing saved yet" else "${entries.size} symbols",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val next = !notifications
                    if (next && !hasNotificationPermission(context)) {
                        requestNotificationPermission(context)
                        return@IconButton
                    }
                    notifications = next
                    setNotificationsEnabled(context, next)
                }
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = if (notifications) "Notifications on" else "Notifications off",
                    tint = if (notifications) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            EmptyView(
                title = "No symbols yet",
                message = "Open a symbol from Home and tap the star to save it here.",
                modifier = Modifier.padding(top = 48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.symbol }) { entry ->
                    SymbolCard(
                        symbol = entry.symbol,
                        onClick = { onOpenSymbol(entry.symbol) },
                        trailing = {
                            IconButton(onClick = { viewModel.remove(entry.symbol) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove ${entry.symbol}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun requestNotificationPermission(context: Context) {
    (context as? android.app.Activity)?.requestPermissions(
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        1
    )
}
