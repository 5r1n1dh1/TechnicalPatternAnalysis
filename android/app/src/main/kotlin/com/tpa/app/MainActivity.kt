package com.tpa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChartLine
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.network.NetworkModule
import com.tpa.app.ui.symbol.ChartScreen
import com.tpa.app.ui.symbol.ChartViewModel
import com.tpa.app.ui.symbol.FundamentalsScreen
import com.tpa.app.ui.symbol.FundamentalsViewModel
import com.tpa.app.ui.symbol.TechnicalScreen
import com.tpa.app.ui.symbol.TechnicalViewModel
import com.tpa.app.ui.theme.TechnicalPatternAnalysisTheme
import com.tpa.app.data.wishlist.WishlistRepository
import com.tpa.app.ui.wishlist.WishlistScreen
import com.tpa.app.ui.wishlist.WishlistViewModel
import com.tpa.app.ui.theme.glassSurface

private const val DEFAULT_SYMBOL = "RELIANCE"

enum class Tab { Search, Wishlist, Charts }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TechnicalPatternAnalysisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TpaNavHost()
                }
            }
        }
    }
}

@Composable
fun TpaNavHost() {
    var selectedTab by remember { mutableStateOf(Tab.Search) }
    var stackedSymbol by remember { mutableStateOf<String?>(null) }

    // Global back: from stacked symbol back to tab root
    BackHandler(enabled = stackedSymbol != null) {
        stackedSymbol = null
    }

    Scaffold(
        bottomBar = {
            GlassBottomBar(
                selected = selectedTab,
                onSelect = { if (stackedSymbol == null) selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                stackedSymbol != null -> SymbolDetailScreen(
                    symbol = stackedSymbol!!,
                    onBack = { stackedSymbol = null }
                )
                else -> when (selectedTab) {
                    Tab.Search -> SearchTab(onOpen = { stackedSymbol = it })
                    Tab.Wishlist -> WishlistTab(onOpen = { stackedSymbol = it })
                    Tab.Charts -> ChartsTab()
                }
            }
        }
    }
}

@Composable
fun GlassBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(68.dp)
            .blur(8.dp)
            .glassSurface()
    ) {
        val items = listOf(
            Tab.Search to Pair(Icons.Default.Search, "Search"),
            Tab.Wishlist to Pair(Icons.Default.Star, "Wishlist"),
            Tab.Charts to Pair(Icons.Default.ChartLine, "Charts")
        )
        items.forEach { (tab, pair) ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(pair.first, contentDescription = pair.second, modifier = Modifier.size(24.dp)) },
                label = { Text(pair.second, fontSize = 11.sp) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun SearchTab(onOpen: (String) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Search", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { onOpen(DEFAULT_SYMBOL) }) {
                Text("Open $DEFAULT_SYMBOL")
            }
        }
    }
}

@Composable
fun WishlistTab(onOpen: (String) -> Unit) {
    val repo = remember { WishlistRepository(androidx.compose.ui.platform.LocalContext.current) }
    val vm = remember { WishlistViewModel(repo) }
    WishlistScreen(viewModel = vm, onOpen = onOpen)
}

@Composable
fun ChartsTab() {
    val api = NetworkModule.provideApi()
    val vm = remember { ChartViewModel(api) }
    GlassCard(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ChartScreen(viewModel = vm, onBack = {})
    }
}

@Composable
fun SymbolDetailScreen(symbol: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Glass app bar with global back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Search, contentDescription = "Back", tint = Color(0xFF059669))
                // Using a back arrow symbol concept; keeping icon simple per existing imports
            }
            Text(symbol, fontSize = 20.sp, color = Color(0xFF0F172A))
        }
        // Inner tabs: Fundamentals / Charts / Technicals
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("Fundamentals", "Charts", "Technicals").forEachIndexed { i, label ->
                TextButton(
                    onClick = {},
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, fontSize = 12.sp, color = if (i == 1) Color(0xFF059669) else Color(0xFF94A3B8))
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            GlassCard(modifier = Modifier.fillMaxSize()) {
                SymbolContent(symbol)
            }
        }
    }
}

@Composable
fun SymbolContent(symbol: String) {
    val api = NetworkModule.provideApi()
    val fundVm = remember { FundamentalsViewModel(api, symbol) }
    val techVm = remember { TechnicalViewModel(api) }
    // Default inner tab = Charts (index 1) per original; kept for compatibility
    val chartVm = remember { ChartViewModel(api) }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        FundamentalsTabView(fundVm)
    }
}

@Composable
private fun FundamentalsTabView(vm: FundamentalsViewModel) {
    val state by vm.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.loading && state.data == null) {
            CircularProgressIndicatorPlaceholder()
        } else if (state.data != null) {
            FundamentalsScreen(data = state.data)
        } else {
            Text(state.error?.let { "Fundamentals unavailable: $it" } ?: "Fundamentals unavailable", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun CircularProgressIndicatorPlaceholder() {
    // Minimal placeholder; full indicator kept from original import if needed
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Text("…", fontSize = 20.sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier
            .graphicsLayer { alpha = 0.92f; clip = true }
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f), RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
