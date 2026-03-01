package com.futsch1.medtimer.statistics.ui.charts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.model.TakenSkippedData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TakenSkippedPieChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun data(
        taken: Int = 0,
        skipped: Int = 0,
        title: String = "Last 7 days"
    ) = TakenSkippedData(
        taken = taken,
        skipped = skipped,
        title = title
    )

    @Test
    fun `chart displays title`() {
        composeTestRule.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data(taken = 7, skipped = 3))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

    @Test
    fun `chart renders with normal data`() {
        composeTestRule.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data(taken = 7, skipped = 3))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

    @Test
    fun `chart renders with both zero`() {
        composeTestRule.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data(taken = 0, skipped = 0, title = "Total"))
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun `chart renders with 100 percent taken`() {
        composeTestRule.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data(taken = 10, skipped = 0, title = "Total"))
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun `chart renders with 100 percent skipped`() {
        composeTestRule.setContent {
            MedTimerTheme {
                TakenSkippedPieChart(data = data(taken = 0, skipped = 5))
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

}
