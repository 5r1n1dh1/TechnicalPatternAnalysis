package com.tpa.app.ui.symbol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
import com.tpa.app.network.Detection
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.chart.allowedIntervals
import com.tpa.app.ui.chart.allowedRanges
import com.tpa.app.ui.components.ProgressBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ChartViewModel(
    private val api: TpaApi,
    initialSymbol: String = ""
) : ViewModel() {

    private val _state = MutableStateFlow(ChartState())
    val state: StateFlow<ChartState> = _state

    init {
        if (initialSymbol.isNotBlank()) setSymbol(initialSymbol)
    }

    fun setSymbol(symbol: String) {
        val cleaned = symbol.trim().uppercase()
        if (cleaned.isBlank()) return
        _state.value = _state.value.copy(
            symbol = cleaned,
            selectedPattern = null,
            patternFilter = null,
            tradePlan = null,
            tradePlanError = null
        )
        refreshAll()
    }

    /** Candles, trend, sector and pattern detection for the current pair. */
    fun refreshAll() {
        fetchCandles()
        loadTrend()
        loadMeta()
        loadPatterns()
    }

    /** Error/404 escape hatch: always returns to a pair the server can serve. */
    fun resetToDefault() {
        _state.value = _state.value.copy(
            interval = Interval("1d"),
            range = Range("1Y"),
            error = null,
            selectedPattern = null,
            patternFilter = null,
            tradePlan = null,
            patternsError = null,
            tradePlanError = null
        )
        refreshAll()
    }

    fun fetchCandles() {
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(isLoading = true, error = null)
            if (current.symbol.isBlank()) {
                _state.value = current.copy(isLoading = false)
                return@launch
            }

            try {
                val response = ProgressBus.track {
                    api.getCandles(
                        symbol = current.symbol,
                        interval = current.interval.value,
                        range = current.range.value
                    )
                }

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
                        error = describe("Price data", response.code(), current),
                        candles = emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message ?: "request failed"}",
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
                val trend = ProgressBus.track {
                    api.getTrend(
                        symbol = current.symbol,
                        interval = current.interval.value,
                        range = current.range.value
                    )
                }
                _state.value = _state.value.copy(trend = trend.label, trendStrength = trend.strength)
            } catch (_: Exception) {
                // Header stays usable without a trend label.
            }
        }
    }

    /** Resolve sector from /api/search for the chart header. */
    fun loadMeta() {
        viewModelScope.launch {
            val current = _state.value
            if (current.symbol.isBlank()) return@launch
            try {
                val results = ProgressBus.track { api.search(current.symbol) }
                val match = results.firstOrNull { it.symbol.equals(current.symbol, ignoreCase = true) }
                    ?: results.firstOrNull()
                if (match != null) {
                    _state.value = _state.value.copy(sector = match.sector)
                }
            } catch (_: Exception) {
                // Sector is optional.
            }
        }
    }

    fun updateInterval(interval: Interval) {
        val nextRange = Range(
            _state.value.range.value.takeIf { it in allowedRanges(interval.value) } ?: "1Y"
        )
        applySelection(interval = interval, range = nextRange)
    }

    fun updateRange(range: Range) {
        val nextInterval = Interval(
            _state.value.interval.value.takeIf { it in allowedIntervals(range.value) } ?: "1d"
        )
        applySelection(interval = nextInterval, range = range)
    }

    private fun applySelection(interval: Interval, range: Range) {
        val filter = _state.value.patternFilter
        _state.value = _state.value.copy(
            interval = interval,
            range = range,
            // The old overlay belongs to the previous pair.
            selectedPattern = null,
            tradePlan = null,
            tradePlanError = null,
            patternsError = null
        )
        fetchCandles()
        loadTrend()
        loadMeta()
        // Re-run detection for the new pair, keeping the chosen pattern filter.
        loadPatterns(filter)
    }

    /**
     * [filter] is a server pattern enum ("head_and_shoulders", ...) or null for
     * all detections. Never pass a detection id here.
     */
    fun loadPatterns(filter: String? = null) {
        val current = _state.value
        _state.value = current.copy(
            patternsLoading = true,
            patternsError = null,
            patternFilter = filter,
            selectedPattern = if (filter == null) current.selectedPattern else null
        )

        viewModelScope.launch {
            try {
                val response = ProgressBus.track {
                    api.getPatterns(
                        symbol = current.symbol,
                        interval = current.interval.value,
                        range = current.range.value,
                        pattern = filter?.takeIf { it.isNotBlank() && it != "all" }
                    )
                }

                if (response.isSuccessful) {
                    val patterns = response.body()?.detections ?: emptyList()
                    val keep = _state.value.selectedPattern
                        ?.takeIf { id -> patterns.any { it.id == id } }
                    _state.value = _state.value.copy(
                        patternsLoading = false,
                        patternsError = null,
                        detections = patterns,
                        selectedPattern = keep
                    )
                    if (keep == null && _state.value.tradePlan != null) {
                        _state.value = _state.value.copy(tradePlan = null)
                    }
                } else {
                    _state.value = _state.value.copy(
                        patternsLoading = false,
                        detections = emptyList(),
                        selectedPattern = null,
                        tradePlan = null,
                        patternsError = describe("Pattern scan", response.code(), current)
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    patternsLoading = false,
                    patternsError = "Network error: ${e.message ?: "request failed"}"
                )
            }
        }
    }

    fun setPatternFilter(filter: String?) {
        _state.value = _state.value.copy(
            selectedPattern = null,
            tradePlan = null,
            tradePlanError = null
        )
        loadPatterns(filter)
    }

    /** Load the trade plan for a real server pattern name (head_and_shoulders, ...). */
    fun loadTradePlan(patternName: String) {
        if (patternName.isBlank() || patternName == "all") return
        viewModelScope.launch {
            _state.value = _state.value.copy(tradePlanLoading = true, tradePlanError = null)

            try {
                val response = ProgressBus.track {
                    api.getTradePlan(symbol = _state.value.symbol, pattern = patternName)
                }

                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        tradePlanLoading = false,
                        tradePlanError = null,
                        tradePlan = response.body()?.active?.firstOrNull()
                    )
                } else {
                    _state.value = _state.value.copy(
                        tradePlanLoading = false,
                        tradePlanError = "Trade plan unavailable (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    tradePlanLoading = false,
                    tradePlanError = "Network error: ${e.message ?: "request failed"}"
                )
            }
        }
    }

    fun selectPattern(patternId: String, patternName: String? = null) {
        _state.value = _state.value.copy(
            selectedPattern = patternId,
            tradePlan = null,
            tradePlanError = null
        )
        if (patternName != null) {
            loadTradePlan(patternName)
        } else {
            _state.value = _state.value.copy(tradePlan = null)
        }
    }

    private fun describe(label: String, code: Int, state: ChartState): String = when (code) {
        400 -> "$label: ${state.interval.value} candles are not available for ${state.range.value}."
        404 -> "$label: no data for ${state.interval.value} / ${state.range.value}."
        in 500..599 -> "$label: server busy ($code). Try again."
        else -> "$label: request failed ($code)."
    }
}
