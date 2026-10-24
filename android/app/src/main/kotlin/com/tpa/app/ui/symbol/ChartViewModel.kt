package com.tpa.app.ui.symbol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
import com.tpa.app.network.Detection
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.Interval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

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
        loadTrend()
        loadMeta()
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
                    val eod = candles.lastOrNull()?.c?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null,
                        candles = candles,
                        eodPrice = eod
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

    /** /api/trend -> {"label","strength","ema20","ema50","adx"}. */
    fun loadTrend() {
        viewModelScope.launch {
            val current = _state.value
            if (current.symbol.isBlank()) return@launch
            try {
                val trend = api.getTrend(
                    symbol = current.symbol,
                    interval = current.interval.value,
                    range = current.range.value
                )
                _state.value = _state.value.copy(trend = trend.label, trendStrength = trend.strength)
            } catch (_: Exception) {
                // Leave any previously shown trend; header stays usable.
            }
        }
    }

    /** Resolve symbol name/sector from /api/search for the chart header. */
    fun loadMeta() {
        viewModelScope.launch {
            val current = _state.value
            if (current.symbol.isBlank()) return@launch
            try {
                val results = api.search(current.symbol)
                val match = results.firstOrNull { it.symbol.equals(current.symbol, ignoreCase = true) }
                    ?: results.firstOrNull()
                if (match != null) {
                    _state.value = _state.value.copy(sector = match.sector)
                }
            } catch (_: Exception) {
                // Sector is optional; keep whatever we have.
            }
        }
    }

    fun updateInterval(interval: Interval) {
        _state.value = _state.value.copy(interval = interval)
        fetchCandles()
        loadPatterns()
        loadTrend()
    }

    fun updateRange(range: Range) {
        _state.value = _state.value.copy(range = range)
        fetchCandles()
        loadPatterns()
        loadTrend()
    }

    fun loadPatterns() {
        viewModelScope.launch {
            _state.value = _state.value.copy(patternsLoading = true, patternsError = null)

            try {
                val current = _state.value
                // Only forward a real server pattern enum; "all"/null means no filter.
                val patternFilter = current.selectedPattern
                    ?.takeIf { it.isNotBlank() && it != "all" && it != "ALL" }
                val response = api.getPatterns(
                    symbol = current.symbol,
                    interval = current.interval.value,
                    range = current.range.value,
                    pattern = patternFilter
                )

                if (response.isSuccessful) {
                    val patterns = response.body()?.detections ?: emptyList()
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

    /** Load the trade plan for a real server pattern name (head_and_shoulders, ...). */
    fun loadTradePlan(patternName: String) {
        if (patternName.isBlank() || patternName == "all") return
        viewModelScope.launch {
            _state.value = _state.value.copy(tradePlanLoading = true, tradePlanError = null)

            try {
                val response = api.getTradePlan(
                    symbol = _state.value.symbol,
                    pattern = patternName
                )

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        tradePlanLoading = false,
                        tradePlanError = null,
                        tradePlan = response.body()?.active?.firstOrNull()
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

    fun selectPattern(patternId: String, patternName: String? = null) {
        _state.value = _state.value.copy(selectedPattern = patternId)
        if (patternName != null) {
            loadTradePlan(patternName)
        } else {
            _state.value = _state.value.copy(tradePlan = null)
        }
    }
}
