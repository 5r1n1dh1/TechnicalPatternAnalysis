package com.tpa.app.ui.symbol

import androidx.compose.runtime.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
import com.tpa.app.network.Detection
import com.tpa.app.network.TradePlanResponse
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.chart.ChartState
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.Interval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class ChartViewModel(
    private val api: TpaApi
) : ViewModel() {

    private val _state = MutableStateFlow(ChartState())
    val state: StateFlow<ChartState> = _state

    init {
        fetchCandles()
    }

    fun fetchCandles() {
        viewModelScope.launch {
            _state.value = ChartState(isLoading = true)

            try {
                val response = api.getCandles(
                    symbol = _state.value.symbol,
                    interval = _state.value.interval,
                    range = _state.value.range
                )

                if (response.isSuccessful) {
                    val candles = response.body()?.candles ?: emptyList()
                    _state.value = ChartState(
                        isLoading = false,
                        error = null,
                        candles = candles,
                        symbol = _state.value.symbol,
                        sector = _state.value.sector,
                        trend = _state.value.trend,
                        eodPrice = _state.value.eodPrice,
                        interval = _state.value.interval,
                        range = _state.value.range
                    )
                } else {
                    _state.value = ChartState(
                        isLoading = false,
                        error = "Failed to fetch data: ${response.code()}",
                        candles = emptyList(),
                        symbol = _state.value.symbol,
                        sector = _state.value.sector,
                        trend = _state.value.trend,
                        eodPrice = _state.value.eodPrice,
                        interval = _state.value.interval,
                        range = _state.value.range
                    )
                }
            } catch (e: Exception) {
                _state.value = ChartState(
                    isLoading = false,
                    error = "Network error: ${e.message}",
                    candles = emptyList(),
                    symbol = _state.value.symbol,
                    sector = _state.value.sector,
                    trend = _state.value.trend,
                    eodPrice = _state.value.eodPrice,
                    interval = _state.value.interval,
                    range = _state.value.range
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