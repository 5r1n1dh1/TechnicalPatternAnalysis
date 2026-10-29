package com.tpa.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import com.tpa.app.network.SearchResult
import com.tpa.app.ui.components.EmptyView
import com.tpa.app.ui.components.SymbolCard

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        placeholder = { Text("Search NSE symbol or name") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange(""); focus.clearFocus() }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(onSearch = {
            onSearch()
            focus.clearFocus()
        }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
        )
    )
}

@Composable
fun SearchResults(
    state: SearchUiState,
    onOpenSymbol: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            state.loading -> Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Searching NSE symbols…",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error != null -> Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.error,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRetry) { Text("Retry search") }
            }

            state.results.isEmpty() -> EmptyView(
                title = "No matches",
                message = "Try a symbol like RELIANCE or a company name."
            )

            else -> state.results.forEach { result ->
                ResultCard(result = result, onClick = { onOpenSymbol(result.symbol) })
            }
        }
    }
}

@Composable
private fun ResultCard(result: SearchResult, onClick: () -> Unit) {
    SymbolCard(
        symbol = result.symbol,
        name = result.name,
        sector = result.sector,
        onClick = onClick
    )
}
