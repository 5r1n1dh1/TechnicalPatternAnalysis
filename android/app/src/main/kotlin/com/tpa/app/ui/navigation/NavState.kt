package com.tpa.app.ui.navigation

enum class MainTab(val label: String) {
    Home("Home"),
    Wishlist("Wishlist"),
    Charts("Charts")
}

enum class SymbolTab(val label: String) {
    Chart("Chart"),
    Fundamentals("Fundamentals"),
    Technicals("Technicals")
}

data class SymbolRoute(
    val symbol: String,
    val tab: SymbolTab = SymbolTab.Chart
)

/**
 * Pure navigation value. Tabs always win: selecting a tab closes any open
 * symbol detail, which is the behaviour users expect from a bottom bar.
 */
data class NavState(
    val tab: MainTab = MainTab.Home,
    val detail: SymbolRoute? = null
) {
    fun selectTab(next: MainTab): NavState = NavState(tab = next, detail = null)

    fun openSymbol(symbol: String, tab: SymbolTab = SymbolTab.Chart): NavState {
        val clean = symbol.trim().uppercase()
        if (clean.isBlank()) return this
        return copy(detail = SymbolRoute(clean, tab))
    }

    fun selectSymbolTab(tab: SymbolTab): NavState =
        copy(detail = detail?.copy(tab = tab))

    fun back(): NavState = if (detail != null) copy(detail = null) else this
}
