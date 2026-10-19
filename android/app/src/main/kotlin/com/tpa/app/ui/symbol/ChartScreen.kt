package com.tpa.app.ui.symbol

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.tpa.app.R
import com.tpa.app.ui.chart.IntervalDropdown
import com.tpa.app.ui.chart.RangeDropdown
import com.tpa.app.ui.chart.PatternDropdown
import com.tpa.app.network.NetworkModule
import com.tpa.app.network.TpaApi
import android.graphics.Color as AndroidColor
import com.tpa.app.ui.theme.glassSurface
import com.tpa.app.ui.chart.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChartScreen(viewModel: ChartViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) { Text("Chart") }
}
