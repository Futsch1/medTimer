package com.futsch1.medtimer.statistics.ui.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.futsch1.medtimer.core.designsystem.MedTimerTheme
import com.futsch1.medtimer.statistics.model.MedicinePerDayData
import com.futsch1.medtimer.statistics.model.MedicineSeriesData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
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
                days = persistentListOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                ),
                series = persistentListOf(
                    MedicineSeriesData("Aspirin", persistentListOf(1, 2, 1), Color(0xFF003f5c)),
                    MedicineSeriesData("Ibuprofen", persistentListOf(0, 1, 1), Color(0xFF2f4b7c)),
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
                days = persistentListOf(),
                series = persistentListOf(),
            )
        )
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `chart renders with single medicine`() {
        setChartContent(
            MedicinePerDayData(
                title = "Last 7 days",
                days = persistentListOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                    LocalDate.of(2023, 12, 4),
                    LocalDate.of(2023, 12, 5),
                ),
                series = persistentListOf(
                    MedicineSeriesData("Aspirin", persistentListOf(1, 2, 0, 3, 1), Color(0xFF003f5c)),
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
                days = persistentListOf(
                    LocalDate.of(2023, 12, 1),
                    LocalDate.of(2023, 12, 2),
                    LocalDate.of(2023, 12, 3),
                ),
                series = persistentListOf(
                    MedicineSeriesData("Med A", persistentListOf(1, 2, 1), Color(0xFF003f5c)),
                    MedicineSeriesData("Med B", persistentListOf(2, 1, 0), Color(0xFF2f4b7c)),
                    MedicineSeriesData("Med C", persistentListOf(0, 1, 3), Color(0xFF665191)),
                ),
            )
        )
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `null data renders empty placeholder`() {
        composeTestRule.setContent {
            MedTimerTheme {
                MedicinePerDayBarChart(data = null)
            }
        }
        composeTestRule.onRoot().assertExists()
    }
}
