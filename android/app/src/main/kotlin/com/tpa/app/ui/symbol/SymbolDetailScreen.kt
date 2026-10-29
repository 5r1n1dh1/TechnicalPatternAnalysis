package com.tpa.app.ui.symbol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpa.app.AppContainer
import com.tpa.app.ui.navigation.SymbolTab
import com.tpa.app.ui.theme.glassSurface
import com.tpa.app.ui.wishlist.WishlistViewModel

@Composable
fun SymbolDetailScreen(
    symbol: String,
    selectedTab: SymbolTab,
    onTabChange: (SymbolTab) -> Unit,
    onBack: () -> Unit,
    container: AppContainer,
    wishlistVm: WishlistViewModel
) {
    val entries by wishlistVm.entries.collectAsState()
    val saved = entries.any { it.symbol.equals(symbol, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "NSE · technical pattern scan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { wishlistVm.toggle(symbol) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (saved) "Remove from wishlist" else "Add to wishlist",
                    tint = if (saved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (saved) "Saved" else "Save",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp),
                    color = if (saved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SegmentedTabs(selected = selectedTab, onSelect = onTabChange)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                SymbolTab.Chart -> key(symbol) {
                    val vm = remember(symbol) { ChartViewModel(container.api, symbol) }
                    ChartTab(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
                SymbolTab.Fundamentals -> key(symbol) {
                    val vm = remember(symbol) { FundamentalsViewModel(container.api, symbol) }
                    FundamentalsTab(viewModel = vm)
                }
                SymbolTab.Technicals -> key(symbol) {
                    val vm = remember(symbol) { TechnicalViewModel(container.api) }
                    TechnicalsTab(viewModel = vm, symbol = symbol)
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(selected: SymbolTab, onSelect: (SymbolTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassSurface(RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SymbolTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
