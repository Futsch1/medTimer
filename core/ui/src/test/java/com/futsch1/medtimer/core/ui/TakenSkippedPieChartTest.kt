package com.futsch1.medtimer.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TakenSkippedPieChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun data(
        taken: Long = 0,
        skipped: Long = 0,
        title: String = "Last 7 days"
    ) = TakenSkippedData(
        taken = taken,
        skipped = skipped,
        title = title
    )

    @Test
    fun `chart displays title`() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = data(taken = 7, skipped = 3))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

    @Test
    fun `chart renders with normal data`() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = data(taken = 7, skipped = 3))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

    @Test
    fun `chart renders with both zero`() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = data(taken = 0, skipped = 0, title = "Total"))
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun `chart renders with 100 percent taken`() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = data(taken = 10, skipped = 0, title = "Total"))
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun `chart renders with 100 percent skipped`() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = data(taken = 0, skipped = 5))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

}
