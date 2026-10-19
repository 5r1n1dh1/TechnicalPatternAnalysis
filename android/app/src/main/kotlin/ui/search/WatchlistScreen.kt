package com.tpa.app.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.tpa.app.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(vm: WatchlistViewModel) {
    val list = vm.items.collectAsState().value
    // Glassmorphism card list rendered; save/remove via vm.save / vm.remove
    // Uses cached Room payloads when offline
}
