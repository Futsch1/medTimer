package com.futsch1.medtimer.statistics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.futsch1.medtimer.core.ui.TakenSkippedData
import com.futsch1.medtimer.core.ui.TakenSkippedPieChart
import org.junit.Rule
import org.junit.Test

class TakenSkippedPieChartInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chartRendersWithRealisticData() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(
                    data = TakenSkippedData(
                        taken = 42,
                        skipped = 8,
                        title = "Last 7 days",
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()
    }

    @Test
    fun chartRendersTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(
                    data = TakenSkippedData(
                        taken = 10,
                        skipped = 5,
                        title = "Total",
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun chartRecomposesWithNewData() {
        val mutableData = androidx.compose.runtime.mutableStateOf(
            TakenSkippedData(
                taken = 5,
                skipped = 5,
                title = "Last 7 days",
                takenLabel = "Taken",
                skippedLabel = "Skipped"
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                TakenSkippedPieChart(data = mutableData.value)
            }
        }
        composeTestRule.onNodeWithText("Last 7 days").assertIsDisplayed()

        mutableData.value = TakenSkippedData(
            taken = 10,
            skipped = 2,
            title = "Total",
            takenLabel = "Taken",
            skippedLabel = "Skipped"
        )
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }
}
