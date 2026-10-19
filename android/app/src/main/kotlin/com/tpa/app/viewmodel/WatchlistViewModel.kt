package com.tpa.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.database.WatchlistDao
import com.tpa.app.database.WatchlistEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(private val dao: WatchlistDao) : ViewModel() {
    val items: StateFlow<List<WatchlistEntity>> = dao.getAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun save(symbol: String) {
        viewModelScope.launch {
            if (dao.getBySymbol(symbol) == null) dao.insert(WatchlistEntity(symbol))
        }
    }

    fun remove(symbol: String) {
        viewModelScope.launch {
            dao.getBySymbol(symbol)?.let { dao.delete(it) }
        }
    }
}
