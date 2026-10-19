package com.tpa.app.ui.symbol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.FundamentalsResponse
import com.tpa.app.network.TpaApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FundamentalsUiState(
    val loading: Boolean = false,
    val data: FundamentalsResponse? = null,
    val error: String? = null
)

class FundamentalsViewModel(
    private val api: TpaApi,
    private val symbol: String
) : ViewModel() {

    private val _state = MutableStateFlow(FundamentalsUiState())
    val state: StateFlow<FundamentalsUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = FundamentalsUiState(loading = true, data = null, error = null)
            try {
                val resp = api.getFundamentals(symbol)
                if (resp.isSuccessful) {
                    _state.value = FundamentalsUiState(
                        loading = false,
                        data = resp.body(),
                        error = null
                    )
                } else {
                    _state.value = FundamentalsUiState(
                        loading = false,
                        data = null,
                        error = "HTTP ${resp.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = FundamentalsUiState(
                    loading = false,
                    data = null,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}
