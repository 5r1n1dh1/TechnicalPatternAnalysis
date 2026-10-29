package com.tpa.app.ui.symbol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tpa.app.network.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FundamentalsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fundamentalsScreenShowsHeaderRatiosAndFlags() {
        val mock = FundamentalsResponse(
            header = Header(name = "RELIANCE", sector = "Energy", mcap = 2.1e11, w52Range = "1300-1500"),
            valuation = Valuation(pe_ttm = 18.5, pb = 2.3),
            ratios = listOf(
                RatioItem("PE", 18.5, "computed"),
                RatioItem("PB", 2.3, "reported"),
                RatioItem("EV/EBITDA", null, "missing")
            ),
            redFlags = RedFlags(leverage = true, earningsQuality = false, growth = false, management = false),
            quarterlyResults = listOf(
                QuarterlyResult("Q3 2026", 1.2e10, 1.1e9, 35.0, 5.0)
            ),
            shareholding = Shareholding(promoter = 57.0, fii = 12.0, dii = 8.0, public = 23.0),
            dcf = DcfEstimate(wacc = 0.10, terminalGrowth = 0.04, equityValuePerShare = 1520.0, upsidePercent = 12.5)
        )

        composeTestRule.setContent {
            FundamentalsScreen(data = mock)
        }

        composeTestRule.onNodeWithText("RELIANCE").assertExists()
        composeTestRule.onNodeWithText("Energy").assertExists()
        composeTestRule.onNodeWithText("18.5 (computed)").assertExists()
        composeTestRule.onNodeWithText("2.3 (reported)").assertExists()
        composeTestRule.onNodeWithText("— (missing)").assertExists()
        composeTestRule.onNodeWithText("leverage (hard)").assertExists()
    }
}
