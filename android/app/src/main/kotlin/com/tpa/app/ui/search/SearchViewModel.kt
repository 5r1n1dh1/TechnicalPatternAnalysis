package com.tpa.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.SearchResult
import com.tpa.app.network.TpaApi
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

    fun onQueryChange(value: String) {
        job?.cancel()
        val query = value.trimStart()
        _state.update { it.copy(query = query, error = null, loading = query.isNotBlank()) }

        if (query.isBlank()) {
            _state.value = SearchUiState()
            return
        }

        job = viewModelScope.launch {
            delay(300)
            try {
                val results = api.search(query.trim(), 20)
                _state.update { it.copy(loading = false, results = results) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, results = emptyList(), error = e.message ?: "Search failed")
                }
            }
        }
    }

    fun clear() {
        job?.cancel()
        _state.value = SearchUiState()
    }
}
