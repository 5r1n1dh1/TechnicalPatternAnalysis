package com.tpa.app.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.data.wishlist.WishlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WishlistViewModel(repo: WishlistRepository) : ViewModel() {
    val entries: StateFlow<List<com.tpa.app.data.wishlist.WishlistEntry>> =
        repo.all().map { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
