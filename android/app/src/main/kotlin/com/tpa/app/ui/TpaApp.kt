package com.tpa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.tpa.app.AppContainer
import com.tpa.app.R
import com.tpa.app.ui.chart.ChartsTab
import com.tpa.app.ui.components.AppBackground
import com.tpa.app.ui.home.HomeScreen
import com.tpa.app.ui.navigation.MainTab
import com.tpa.app.ui.navigation.NavState
import com.tpa.app.ui.navigation.SymbolRoute
import com.tpa.app.ui.navigation.SymbolTab
import com.tpa.app.ui.symbol.SymbolDetailScreen
import com.tpa.app.ui.theme.glassBar
import com.tpa.app.ui.wishlist.WishlistScreen
import com.tpa.app.ui.wishlist.WishlistViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze

private val NavSaver = listSaver<NavState, String>(
    save = {
        listOf(
            it.tab.name,
            it.detail?.symbol.orEmpty(),
            it.detail?.tab?.name.orEmpty()
        )
    },
    restore = { saved ->
        val detail = saved.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { symbol ->
            SymbolRoute(
                symbol = symbol,
                tab = saved.getOrNull(2)
                    ?.let { runCatching { SymbolTab.valueOf(it) }.getOrNull() }
                    ?: SymbolTab.Chart
            )
        }
        NavState(
            tab = saved.firstOrNull()
                ?.let { runCatching { MainTab.valueOf(it) }.getOrNull() }
                ?: MainTab.Home,
            detail = detail
        )
    }
)

@Composable
fun TpaApp(container: AppContainer) {
    var nav by rememberSaveable(stateSaver = NavSaver) { mutableStateOf(NavState()) }
    var lastSymbol by rememberSaveable { mutableStateOf("RELIANCE") }
    val hazeState = remember { HazeState() }
    val wishlistVm = remember(container) { WishlistViewModel(container.wishlist) }

    val openSymbol: (String) -> Unit = { symbol ->
        lastSymbol = symbol.trim().uppercase()
        nav = nav.openSymbol(symbol)
    }

    BackHandler(enabled = nav.detail != null) { nav = nav.back() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Everything in this layer is the source that the bottom bar blurs.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    hazeState,
                    HazeStyle(tint = Color.Transparent, blurRadius = 24.dp, noiseFactor = 0f)
                )
        ) {
            AppBackground()
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                val detail = nav.detail
                if (detail != null) {
                    SymbolDetailScreen(
                        symbol = detail.symbol,
                        selectedTab = detail.tab,
                        onTabChange = { nav = nav.selectSymbolTab(it) },
                        onBack = { nav = nav.back() },
                        container = container,
                        wishlistVm = wishlistVm
                    )
                } else {
                    when (nav.tab) {
                        MainTab.Home -> HomeScreen(
                            api = container.api,
                            wishlistVm = wishlistVm,
                            onOpenSymbol = openSymbol,
                            onOpenWishlist = { nav = nav.selectTab(MainTab.Wishlist) }
                        )
                        MainTab.Wishlist -> WishlistScreen(
                            viewModel = wishlistVm,
                            onOpenSymbol = openSymbol
                        )
                        MainTab.Charts -> ChartsTab(
                            api = container.api,
                            initialSymbol = lastSymbol,
                            onSymbolChange = { lastSymbol = it }
                        )
                    }
                }
            }
        }

        GlassBottomBar(
            selected = nav.tab,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter),
            onSelect = { nav = nav.selectTab(it) }
        )
    }
}

@Composable
private fun GlassBottomBar(
    selected: MainTab,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onSelect: (MainTab) -> Unit
) {
    val tabs = listOf(MainTab.Home, MainTab.Wishlist, MainTab.Charts)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassBar(hazeState),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val active = tab == selected
            val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .padding(vertical = 6.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(tab) }
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (tab) {
                    MainTab.Home -> Icon(Icons.Default.Home, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                    MainTab.Wishlist -> Icon(Icons.Default.Star, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                    MainTab.Charts -> Icon(painterResource(R.drawable.ic_chart), contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = tab.label,
                    color = tint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
