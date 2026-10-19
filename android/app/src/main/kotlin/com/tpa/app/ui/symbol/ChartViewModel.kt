package com.tpa.app.ui.symbol

import androidx.compose.runtime.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
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
                        eodPrice = _state.value.eodPrice
                    )
                } else {
                    _state.value = ChartState(
                        isLoading = false,
                        error = "Failed to fetch data: ${response.code()}",
                        candles = emptyList(),
                        symbol = _state.value.symbol,
                        sector = _state.value.sector,
                        trend = _state.value.trend,
                        eodPrice = _state.value.eodPrice
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
                    eodPrice = _state.value.eodPrice
                )
            }
        }
    }

    fun updateInterval(interval: Interval) {
        _state.value = _state.value.copy(interval = interval)
        fetchCandles()
    }

    fun updateRange(range: Range) {
        _state.value = _state.value.copy(range = range)
        fetchCandles()
    }
}