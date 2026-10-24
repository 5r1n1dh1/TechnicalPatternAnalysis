package com.tpa.app.ui.symbol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
import com.tpa.app.network.Detection
import com.tpa.app.network.TradePlanResponse
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.Interval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChartViewModel(
    private val api: TpaApi
) : ViewModel() {

    private val _state = MutableStateFlow(ChartState())
    val state: StateFlow<ChartState> = _state

    init {
        fetchCandles()
    }

    fun setSymbol(symbol: String) {
        val cleaned = symbol.trim().uppercase()
        if (cleaned.isBlank()) return
        _state.value = _state.value.copy(symbol = cleaned, selectedPattern = null, tradePlan = null)
        fetchCandles()
        loadPatterns()
    }

    fun fetchCandles() {
        viewModelScope.launch {
            // Preserve symbol/range/interval and previously loaded data while refreshing.
            _state.value = _state.value.copy(isLoading = true, error = null)

            val current = _state.value
            if (current.symbol.isBlank()) {
                _state.value = current.copy(isLoading = false)
                return@launch
            }

            try {
                val response = api.getCandles(
                    symbol = current.symbol,
                    interval = current.interval.value,
                    range = current.range.value
                )

                if (response.isSuccessful) {
                    val candles = response.body()?.candles ?: emptyList()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        candles = candles
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to fetch data: ${response.code()}",
                        candles = emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message}",
                    candles = emptyList()
                )
            }
        }
    }

    fun updateInterval(interval: Interval) {
        _state.value = _state.value.copy(interval = interval)
        fetchCandles()
        loadPatterns()
    }

    fun updateRange(range: Range) {
        _state.value = _state.value.copy(range = range)
        fetchCandles()
        loadPatterns()
    }

    fun loadPatterns() {
        viewModelScope.launch {
            _state.value = _state.value.copy(patternsLoading = true, patternsError = null)

            try {
                val patternFilter = _state.value.selectedPattern ?: "all"
                val response = api.getPatterns(
                    symbol = _state.value.symbol,
                    interval = _state.value.interval.value,
                    range = _state.value.range.value,
                    pattern = patternFilter
                )

                if (response.isSuccessful) {
                    val patterns = response.body()?.patterns ?: emptyList()
                    _state.value = _state.value.copy(
                        patternsLoading = false,
                        patternsError = null,
                        detections = patterns
                    )
                } else {
                    _state.value = _state.value.copy(
                        patternsLoading = false,
                        patternsError = "Failed to fetch patterns: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    patternsLoading = false,
                    patternsError = "Network error: ${e.message}"
                )
            }
        }
    }

    fun loadTradePlan(patternId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(tradePlanLoading = true, tradePlanError = null)

            try {
                val response = api.getTradePlan(
                    symbol = _state.value.symbol,
                    pattern = patternId
                )

                if (response.isSuccessful) {
                    val tradePlan = response.body()
                    _state.value = _state.value.copy(
                        tradePlanLoading = false,
                        tradePlanError = null,
                        tradePlan = tradePlan
                    )
                } else {
                    _state.value = _state.value.copy(
                        tradePlanLoading = false,
                        tradePlanError = "Failed to fetch trade plan: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    tradePlanLoading = false,
                    tradePlanError = "Network error: ${e.message}"
                )
            }
        }
    }

    fun selectPattern(patternId: String) {
        _state.value = _state.value.copy(selectedPattern = patternId)
        loadTradePlan(patternId)
    }
}