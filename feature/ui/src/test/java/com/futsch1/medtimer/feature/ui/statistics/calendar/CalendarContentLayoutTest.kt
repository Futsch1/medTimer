package com.futsch1.medtimer.feature.ui.statistics.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.BREAKPOINTS_V1
import androidx.window.core.layout.computeWindowSizeClass
import com.futsch1.medtimer.core.ui.theme.MedTimerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.test.assertTrue

/**
 * Verifies the adaptive arrangement of [CalendarContent]: the event panel sits to the right of the
 * calendar on a tablet in landscape, and below it otherwise. WindowAdaptiveInfo is injected so the
 * width signal does not depend on host window metrics. The calendar is anchored by its month-year
 * navigation title (a sibling of the HorizontalCalendar) and the event panel by the synthetic
 * medicine name rendered only inside DayEventsCard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalendarContentLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Matches CalendarNavigationRow's "<FullMonth> <year>" title for the initially-visible month (now).
    private val calendarTitle: String = run {
        val visibleMonth = YearMonth.now()
        "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}"
    }

    @Test
    @Config(qualifiers = "land")
    fun `tablet landscape places the event panel to the right of the calendar`() {
        setCalendar(width = 900.dp, height = 500.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 500f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.left > calendar.left,
            "Expected the event panel right of the calendar; calendar.left=${calendar.left}, event.left=${event.left}",
        )
    }

    @Test
    fun `phone portrait stacks the event panel below the calendar`() {
        setCalendar(width = 420.dp, height = 900.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 420f, heightDp = 900f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.top > calendar.top,
            "Expected the event panel below the calendar; calendar.top=${calendar.top}, event.top=${event.top}",
        )
    }

    @Test
    @Config(qualifiers = "port")
    fun `wide tablet in portrait stacks the event panel below the calendar`() {
        // Width passes the medium breakpoint, so only the orientation guard keeps the layout stacked.
        setCalendar(width = 900.dp, height = 1200.dp, windowSizeClass = BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 900f, heightDp = 1200f))

        val calendar = composeTestRule.onNodeWithText(calendarTitle).getBoundsInRoot()
        val event = composeTestRule.onNode(hasText("Vitamin X 500 mg", substring = true)).getBoundsInRoot()

        assertTrue(
            event.top > calendar.top,
            "Wide tablet in portrait should stack; calendar.top=${calendar.top}, event.top=${event.top}",
        )
    }

    private fun setCalendar(width: Dp, height: Dp, windowSizeClass: WindowSizeClass) {
        val today = LocalDate.now()
        composeTestRule.setContent {
            MedTimerTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    Box(modifier = Modifier.size(width, height)) {
                        CalendarContent(
                            dayEvents = mapOf(
                                today to listOf(
                                    CalendarDayEvent(today.atTime(8, 0), "1 tablet", "Vitamin X 500 mg", CalendarDayEvent.Status.TAKEN),
                                ),
                            ),
                            windowAdaptiveInfo = WindowAdaptiveInfo(
                                windowSizeClass = windowSizeClass,
                                windowPosture = Posture(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
