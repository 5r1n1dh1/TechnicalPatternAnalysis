package com.tpa.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.components.EmptyView
import com.tpa.app.ui.components.SymbolCard
import com.tpa.app.ui.search.SearchField
import com.tpa.app.ui.search.SearchResults
import com.tpa.app.ui.search.SearchViewModel
import com.tpa.app.ui.wishlist.WishlistViewModel

private val QuickSymbols = listOf("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK")

@Composable
fun HomeScreen(
    api: TpaApi,
    wishlistVm: WishlistViewModel,
    onOpenSymbol: (String) -> Unit,
    onOpenWishlist: () -> Unit = {}
) {
    val searchVm = remember(api) { SearchViewModel(api) }
    val searchState by searchVm.state.collectAsState()
    val saved by wishlistVm.entries.collectAsState()
    val searching = searchState.query.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Pattern Lens",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "NSE technical pattern scanner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SearchField(
                query = searchState.query,
                onQueryChange = searchVm::onQueryChange,
                onSearch = {}
            )
        }

        if (searching) {
            item {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SearchResults(state = searchState, onOpenSymbol = onOpenSymbol)
            }
        } else {
            item {
                Text(
                    text = "Quick look",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuickSymbols) { symbol ->
                        AssistChip(
                            onClick = { onOpenSymbol(symbol) },
                            label = { Text(symbol) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Wishlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (saved.isEmpty()) "Nothing saved yet" else "${saved.size} symbols",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenWishlist) { Text("See all") }
                }
            }

            if (saved.isEmpty()) {
                item {
                    EmptyView(
                        title = "Build your watchlist",
                        message = "Search a symbol above and tap the star on its detail page."
                    )
                }
            } else {
                items(saved.take(6), key = { it.symbol }) { entry ->
                    SymbolCard(
                        symbol = entry.symbol,
                        onClick = { onOpenSymbol(entry.symbol) }
                    )
                }
            }
        }
    }
}
