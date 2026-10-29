package com.tpa.app.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.data.wishlist.WishlistEntry
import com.tpa.app.data.wishlist.WishlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WishlistViewModel(private val repo: WishlistRepository) : ViewModel() {
    val entries: StateFlow<List<WishlistEntry>> =
        repo.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(symbol: String) {
        val clean = symbol.trim().uppercase()
        if (clean.isBlank()) return
        viewModelScope.launch { repo.add(clean) }
    }

    fun remove(symbol: String) {
        viewModelScope.launch { repo.remove(symbol) }
    }

    fun toggle(symbol: String) {
        val clean = symbol.trim().uppercase()
        if (clean.isBlank()) return
        viewModelScope.launch {
            if (repo.exists(clean)) repo.remove(clean) else repo.add(clean)
        }
    }

    fun isSaved(symbol: String): Boolean =
        entries.value.any { it.symbol.equals(symbol, ignoreCase = true) }
}
