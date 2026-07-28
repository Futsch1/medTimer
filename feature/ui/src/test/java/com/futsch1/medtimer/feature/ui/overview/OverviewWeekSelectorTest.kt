package com.futsch1.medtimer.feature.ui.overview

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.futsch1.medtimer.core.ui.R as CoreUiR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OverviewWeekSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * `simulatedThrough` starts at [LocalDate.MIN] until the simulation repository first emits, so
     * the very first composition is handed an end date before the start date. The calendar throws on
     * an inverted range, which crashed the app on startup.
     */
    @Test
    fun `renders when the simulated range end has not been emitted yet`() {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewWeekSelector(
                    selectedDay = LocalDate.now(),
                    rangeEnd = LocalDate.MIN,
                    onDaySelected = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(CoreUiR.string.previous_week)).assertExists()
    }

    @Test
    fun `renders with a real simulated range end`() {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewWeekSelector(
                    selectedDay = LocalDate.now(),
                    rangeEnd = LocalDate.now().plusDays(28),
                    onDaySelected = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(CoreUiR.string.previous_week)).assertExists()
    }

    /**
     * Jan 10 2024 is a Wednesday. A Sunday-first week (US) shows Jan 7-13; a Monday-first week
     * (Germany) shows Jan 8-14. Each locale's week must not contain the other's edge day.
     */
    @Test
    @Config(qualifiers = "en-rUS")
    fun `week starts on Sunday for a Sunday-first locale`() {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewWeekSelector(
                    selectedDay = LocalDate.of(2024, 1, 10),
                    rangeEnd = LocalDate.now().plusDays(28),
                    onDaySelected = {},
                )
            }
        }

        val sunday = composeTestRule.onNodeWithText("7").getBoundsInRoot()
        val saturday = composeTestRule.onNodeWithText("13").getBoundsInRoot()
        assertTrue(sunday.left < saturday.left, "Expected Jan 7 (Sun) left of Jan 13 (Sat)")
        composeTestRule.onNodeWithText("14").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `week starts on Monday for a Monday-first locale`() {
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewWeekSelector(
                    selectedDay = LocalDate.of(2024, 1, 10),
                    rangeEnd = LocalDate.now().plusDays(28),
                    onDaySelected = {},
                )
            }
        }

        val monday = composeTestRule.onNodeWithText("8").getBoundsInRoot()
        val sunday = composeTestRule.onNodeWithText("14").getBoundsInRoot()
        assertTrue(monday.left < sunday.left, "Expected Jan 8 (Mon) left of Jan 14 (Sun)")
        composeTestRule.onNodeWithText("7").assertDoesNotExist()
    }

    /** Jan 10 2024 is a Wednesday; with a Sunday-first week its week runs Jan 7-13. */
    @Test
    @Config(qualifiers = "en-rUS")
    fun `previous arrow selects the last day of the previous week when today is not in it`() {
        val captured = renderAndClick(LocalDate.of(2024, 1, 10), CoreUiR.string.previous_week)

        assertEquals(LocalDate.of(2024, 1, 6), captured)
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `next arrow selects the first day of the next week when today is not in it`() {
        val captured = renderAndClick(LocalDate.of(2024, 1, 10), CoreUiR.string.next_week)

        assertEquals(LocalDate.of(2024, 1, 14), captured)
    }

    @Test
    fun `previous arrow selects today when today is in the previous week`() {
        val today = LocalDate.now()
        val captured = renderAndClick(today.plusWeeks(1), CoreUiR.string.previous_week)

        assertEquals(today, captured)
    }

    @Test
    fun `next arrow selects today when today is in the next week`() {
        val today = LocalDate.now()
        val captured = renderAndClick(today.minusWeeks(1), CoreUiR.string.next_week)

        assertEquals(today, captured)
    }

    private fun getString(@StringRes textRes: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(textRes)

    private fun renderAndClick(initialSelectedDay: LocalDate, @StringRes arrowDescription: Int): LocalDate? {
        var captured: LocalDate? = null
        composeTestRule.setContent {
            MedTimerTheme {
                var day by remember { mutableStateOf(initialSelectedDay) }
                OverviewWeekSelector(
                    selectedDay = day,
                    rangeEnd = LocalDate.now().plusDays(28),
                    onDaySelected = {
                        day = it
                        captured = it
                    },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(getString(arrowDescription)).performClick()
        composeTestRule.waitForIdle()
        return captured
    }
}
