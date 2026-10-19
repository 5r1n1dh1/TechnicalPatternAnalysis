package com.tpa.app.ui.symbol

import android.content.Context

import androidx.test.core.app.ApplicationProvider
import com.tpa.app.BuildConfig
import com.tpa.app.network.Candle
import com.tpa.app.network.CandleResponse
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.chart.Interval
import com.tpa.app.ui.chart.Range
import com.tpa.app.ui.symbol.ChartState
import com.tpa.app.ui.symbol.ChartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import retrofit2.Response

@RunWith(Mockito::class)
class ChartViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testContext = ApplicationProvider.getApplicationContext<Context>()
    private val mockApi: TpaApi = Mockito.mock(TpaApi::class.java)
    
    private lateinit var viewModel: ChartViewModel
    private lateinit var api: TpaApi
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.initMocks(this)
        api = mockApi
        viewModel = ChartViewModel(api)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testGetCandlesSuccess() {
        // Create mock response with sample data
        val mockResponse = Response.success(CandleResponse(listOf(
            Candle("2023-01-01", 100.0, 105.0, 95.0, 102.0)
        )))
        
        // Mock api response
        Mockito.`when`(mockApi.getCandles(any(), any(), any())).thenReturn(mockResponse)
        
        // Trigger fetch
        viewModel.fetchCandles()
        
        // Verify state
        val state = viewModel.state.value
        assert(state.isLoading == false)
        assert(state.error == null)
        assert(state.candles.size == 1)
    }
    
    @Test
    fun testGetCandlesFailure() {
        // Mock api response with error
        val mockResponse = Response.error<CandleResponse>(404, null)
        
        // Mock api response
        Mockito.`when`(mockApi.getCandles(any(), any(), any())).thenReturn(mockResponse)
        
        // Trigger fetch
        viewModel.fetchCandles()
        
        // Verify state
        val state = viewModel.state.value
        assert(state.isLoading == false)
        assert(state.error != null)
    }
    
    @Test
    fun testUpdateInterval() {
        val newInterval = Interval("1h")
        viewModel.updateInterval(newInterval)
        
        val state = viewModel.state.value
        assert(state.interval == newInterval)
    }
    
    @Test
    fun testUpdateRange() {
        val newRange = Range("3M")
        viewModel.updateRange(newRange)
        
        val state = viewModel.state.value
        assert(state.range == newRange)
    }
}