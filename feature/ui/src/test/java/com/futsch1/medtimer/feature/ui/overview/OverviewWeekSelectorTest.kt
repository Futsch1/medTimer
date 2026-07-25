package com.futsch1.medtimer.feature.ui.overview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

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

        composeTestRule.onNodeWithTag(OverviewTestTags.PREV_WEEK).assertExists()
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

        composeTestRule.onNodeWithTag(OverviewTestTags.PREV_WEEK).assertExists()
    }
}
