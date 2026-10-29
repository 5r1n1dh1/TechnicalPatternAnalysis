package com.tpa.app.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tpa.app.network.TpaApi
import com.tpa.app.ui.components.GuideButton
import com.tpa.app.ui.symbol.ChartTab
import com.tpa.app.ui.symbol.ChartViewModel

/** Charts tab: keeps its own symbol switcher and survives tab changes via hoisted state. */
@Composable
fun ChartsTab(
    api: TpaApi,
    initialSymbol: String,
    onSymbolChange: (String) -> Unit
) {
    var input by rememberSaveable { mutableStateOf(initialSymbol) }
    var symbol by rememberSaveable { mutableStateOf(initialSymbol) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.uppercase() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Symbol") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = {
                    loadSymbol(input) { symbol = it; onSymbolChange(it) }
                })
            )
            Button(onClick = { loadSymbol(input) { symbol = it; onSymbolChange(it) } }) {
                Text("Load")
            }
            GuideButton("Charts tab", "chart", "eod", "trend", "range", "interval")
        }

        key(symbol) {
            val viewModel = remember(symbol) { ChartViewModel(api, symbol) }
            ChartTab(viewModel = viewModel, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

private inline fun loadSymbol(input: String, onLoaded: (String) -> Unit) {
    val clean = input.trim().uppercase()
    if (clean.isNotBlank()) onLoaded(clean)
}
