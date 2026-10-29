package com.tpa.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class NavStateTest {

    @Test
    fun selectTabClearsOpenSymbol() {
        val state = NavState().openSymbol("reliance")
        val next = state.selectTab(MainTab.Wishlist)

        assertEquals(MainTab.Wishlist, next.tab)
        assertNull(next.detail)
    }

    @Test
    fun openSymbolNormalisesAndKeepsTab() {
        val state = NavState(tab = MainTab.Charts)
        val next = state.openSymbol("  tcs ")

        assertEquals(MainTab.Charts, next.tab)
        assertEquals("TCS", next.detail?.symbol)
        assertEquals(SymbolTab.Chart, next.detail?.tab)
    }

    @Test
    fun openSymbolIgnoresBlank() {
        val state = NavState()
        assertSame(state, state.openSymbol("   "))
    }

    @Test
    fun selectSymbolTabUpdatesOnlyTheOpenSymbol() {
        val state = NavState().openSymbol("INFY")
        val next = state.selectSymbolTab(SymbolTab.Technicals)

        assertEquals(SymbolTab.Technicals, next.detail?.tab)
        assertEquals("INFY", next.detail?.symbol)
    }

    @Test
    fun backClosesDetailAndKeepsTab() {
        val state = NavState(tab = MainTab.Wishlist).openSymbol("HDFCBANK")
        val next = state.back()

        assertEquals(MainTab.Wishlist, next.tab)
        assertNull(next.detail)
    }

    @Test
    fun backWithoutDetailIsNoOp() {
        val state = NavState(tab = MainTab.Home)
        assertSame(state, state.back())
    }
}
