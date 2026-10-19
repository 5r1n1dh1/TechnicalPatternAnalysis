package com.tpa.app.ui.symbol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.TpaApi
import com.tpa.app.network.TechnicalResponse
import com.tpa.app.network.DecisionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TechnicalViewModel(private val api: TpaApi) : ViewModel() {
    private val _technical = MutableStateFlow<TechnicalResponse?>(null)
    val technical: StateFlow<TechnicalResponse?> = _technical

    private val _decision = MutableStateFlow<DecisionResponse?>(null)
    val decision: StateFlow<DecisionResponse?> = _decision

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun load(symbol: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _technical.value = api.getTechnical(symbol).body()
                _decision.value = api.getDecision(symbol).body()
            } catch (_: Exception) { }
            _loading.value = false
        }
    }
}
