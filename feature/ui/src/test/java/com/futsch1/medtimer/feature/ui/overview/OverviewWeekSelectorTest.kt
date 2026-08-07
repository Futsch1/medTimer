package com.futsch1.medtimer.feature.ui.overview

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
     * Weeks are anchored to today rather than to the locale's first day, so yesterday and tomorrow
     * are always adjacent to today without scrolling.
     */
    @Test
    @Config(qualifiers = "de-rDE")
    fun `today is the fourth of the seven days shown, whatever the locale`() {
        val today = LocalDate.now()
        composeTestRule.setContent {
            MedTimerTheme {
                OverviewWeekSelector(
                    selectedDay = today,
                    rangeEnd = today.plusDays(28),
                    onDaySelected = {},
                )
            }
        }

        for (offset in -3L..3L) {
            composeTestRule.onNodeWithTag(OverviewTestTags.day(today.plusDays(offset))).assertExists()
        }
        composeTestRule.onNodeWithTag(OverviewTestTags.day(today.minusDays(4))).assertDoesNotExist()
        composeTestRule.onNodeWithTag(OverviewTestTags.day(today.plusDays(4))).assertDoesNotExist()

        val first = composeTestRule.onNodeWithTag(OverviewTestTags.day(today.minusDays(3))).getBoundsInRoot()
        val last = composeTestRule.onNodeWithTag(OverviewTestTags.day(today.plusDays(3))).getBoundsInRoot()
        assertTrue(first.left < last.left, "Expected today - 3 left of today + 3")
    }

    /** Weeks run today-3..today+3, so the week four weeks out ends at today + 31. */
    @Test
    fun `previous arrow selects the last day of the previous week when today is not in it`() {
        val today = LocalDate.now()
        val captured = renderAndClick(today.plusWeeks(4), CoreUiR.string.previous_week)

        assertEquals(today.plusDays(24), captured)
    }

    @Test
    fun `next arrow selects the first day of the next week when today is not in it`() {
        val today = LocalDate.now()
        val captured = renderAndClick(today.minusWeeks(4), CoreUiR.string.next_week)

        assertEquals(today.minusDays(24), captured)
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
