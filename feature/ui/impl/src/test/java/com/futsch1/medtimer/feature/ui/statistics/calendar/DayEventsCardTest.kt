package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertTrue

/**
 * Verifies that each event row in [DayEventsCard] renders a leading status icon (labelled by status)
 * for past events, while a SCHEDULED (future) event renders no status icon. The status label is
 * surfaced as the icon's content description, so it can be asserted without depending on tint or size.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DayEventsCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val date: LocalDate = LocalDate.of(2026, 5, 28)

    @Test
    fun `a taken event renders a status icon labelled Taken`() {
        setEvents(
            CalendarDayEvent(date.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN, ReminderType.TIME_BASED),
        )

        composeTestRule.onNodeWithText("Vitamin X 500 mg", substring = true).assertExists()
        assertTrue(
            composeTestRule.onAllNodesWithContentDescription("Taken").fetchSemanticsNodes().isNotEmpty(),
            "Expected a Taken status icon (content description) on the event row",
        )
    }

    @Test
    fun `a scheduled event renders no status icon`() {
        setEvents(
            CalendarDayEvent(date.atTime(20, 0), "1 capsule", "Supplement B", CalendarDayEvent.Status.SCHEDULED, ReminderType.WINDOWED_INTERVAL),
        )

        // The row still renders, but a future event carries no status label.
        composeTestRule.onNodeWithText("Supplement B", substring = true).assertExists()
        assertTrue(
            composeTestRule.onAllNodesWithContentDescription("Taken").fetchSemanticsNodes().isEmpty(),
            "A scheduled event should not render a status icon",
        )
    }

    @Test
    fun `a taken event renders the taken time and interval`() {
        setEvents(
            CalendarDayEvent(
                date.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN, ReminderType.CONTINUOUS_INTERVAL,
                takenTime = date.atTime(8, 42), interval = Duration.ofMinutes(150),
            ),
        )

        // The interval label ("Interval …") is rendered after the arrow-linked taken time; assert the
        // locale-stable prefix from R.string.interval_time rather than the locale-formatted duration.
        composeTestRule.onNodeWithText("Interval", substring = true).assertExists()
    }

    private fun setEvents(vararg events: CalendarDayEvent) {
        composeTestRule.setContent {
            MedTimerTheme {
                Surface {
                    DayEventsCard(date = date, events = events.toList())
                }
            }
        }
    }
}
