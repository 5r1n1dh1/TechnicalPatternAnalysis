package com.tpa.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.SearchResult
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.components.ProgressBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class SearchViewModel(private val api: TpaApi) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state
    private var job: Job? = null

    /**
     * Keeps the text exactly as typed (no trimming) so the field never fights
     * the keyboard, then debounces the request.
     */
    fun onQueryChange(value: String) {
        job?.cancel()
        _state.update { it.copy(query = value, error = null, loading = value.isNotBlank()) }

        if (value.isBlank()) {
            _state.value = SearchUiState()
            return
        }

        job = viewModelScope.launch {
            delay(300)
            runSearch(value)
        }
    }

    fun retry() {
        val query = _state.value.query
        if (query.isBlank()) return
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        try {
            val results = ProgressBus.track { api.search(query.trim(), 20) }
            // Ignore a late response for text the user has already replaced.
            if (_state.value.query != query) return
            _state.update { it.copy(loading = false, results = results, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (_state.value.query != query) return
            _state.update {
                it.copy(
                    loading = false,
                    results = emptyList(),
                    error = "Search failed. Check your connection and try again."
                )
            }
        }
    }

    fun clear() {
        job?.cancel()
        _state.value = SearchUiState()
    }
}
