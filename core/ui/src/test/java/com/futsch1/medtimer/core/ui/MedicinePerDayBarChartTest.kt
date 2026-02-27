package com.futsch1.medtimer.core.ui

import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.graphics.Color
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MedicinePerDayBarChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setChartContent(data: MedicinePerDayData) {
        composeTestRule.setContent {
            MedTimerTheme {
                MedicinePerDayBarChart(data = data)
            }
        }
    }

    @Test
    fun `chart renders with sample data`() {
        setChartContent(
            MedicinePerDayData(
                title = "Last 7 days",
                days = listOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                ),
                series = listOf(
                    MedicineSeriesData("Aspirin", listOf(1, 2, 1), Color(0xFF003f5c)),
                    MedicineSeriesData("Ibuprofen", listOf(0, 1, 1), Color(0xFF2f4b7c)),
                ),
            )
        )
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `chart renders with empty data`() {
        setChartContent(
            MedicinePerDayData(
                title = "Last 7 days",
                days = emptyList(),
                series = emptyList(),
            )
        )
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `chart renders with single medicine`() {
        setChartContent(
            MedicinePerDayData(
                title = "Last 7 days",
                days = listOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                    LocalDate.of(2023, 12, 4),
                    LocalDate.of(2023, 12, 5),
                ),
                series = listOf(
                    MedicineSeriesData("Aspirin", listOf(1, 2, 0, 3, 1), null),
                ),
            )
        )
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `chart renders with multiple medicines stacked`() {
        setChartContent(
            MedicinePerDayData(
                title = "Last 7 days",
                days = listOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                ),
                series = listOf(
                    MedicineSeriesData("Med A", listOf(1, 2, 1), null),
                    MedicineSeriesData("Med B", listOf(2, 1, 0), null),
                    MedicineSeriesData("Med C", listOf(0, 1, 3), null),
                ),
            )
        )
        composeTestRule.onRoot().assertExists()
    }
}
