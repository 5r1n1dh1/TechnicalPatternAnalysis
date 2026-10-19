package com.tpa.app.ui.symbol

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tpa.app.network.DecisionResponse
import com.tpa.app.network.ComponentScore
import com.tpa.app.network.RedFlag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecisionCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun decisionCardDisplaysVerdictAndReasons() {
        val mock = DecisionResponse(
            symbol = "RELIANCE",
            verdict = "INVEST",
            confidence = 0.85,
            component_scores = listOf(
                ComponentScore("Fundamental", 78.0),
                ComponentScore("Technical", 72.0),
                ComponentScore("Valuation", 65.0),
                ComponentScore("Pattern", 60.0),
                ComponentScore("ML", 58.0)
            ),
            failing_rubric = listOf("leverage"),
            red_flags = listOf(
                RedFlag("earningsQuality", "Declining QoQ margins", "HIGH")
            ),
            reasons = listOf(
                "Fundamental score 78 + valuation cheap + bullish structure + ML up > down h1/h5"
            )
        )

        composeTestRule.setContent {
            DecisionCard(decision = mock)
        }

        composeTestRule.onNodeWithText("INVEST").assertExists()
        composeTestRule.onNodeWithText("VERDICT").assertExists()
        composeTestRule.onNodeWithText("Fundamental").assertExists()
        composeTestRule.onNodeWithText("Reasons").assertExists()
        composeTestRule.onNodeWithText("earningsQuality").assertExists()
    }
}
