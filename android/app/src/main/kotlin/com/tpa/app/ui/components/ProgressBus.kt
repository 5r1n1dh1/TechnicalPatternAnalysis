package com.tpa.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide count of in-flight network requests. Every ViewModel wraps its
 * calls in [track], so the app shows one global progress bar no matter which
 * screen started the fetch.
 */
object ProgressBus {
    private val _active = MutableStateFlow(0)
    val active: StateFlow<Int> = _active.asStateFlow()

    private fun begin() = _active.update { it + 1 }
    private fun end() = _active.update { (it - 1).coerceAtLeast(0) }

    suspend fun <T> track(block: suspend () -> T): T {
        begin()
        return try {
            block()
        } finally {
            end()
        }
    }
}

/** Thin top bar; visible while any request is running. */
@Composable
fun AppProgressBar(modifier: Modifier = Modifier) {
    val active by ProgressBus.active.collectAsState()
    if (active > 0) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}
