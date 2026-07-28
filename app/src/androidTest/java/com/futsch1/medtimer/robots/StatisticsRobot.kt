package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.core.ui.component.SortableTableTestTags
import com.futsch1.medtimer.feature.ui.statistics.ANALYSIS_RANGES
import com.futsch1.medtimer.feature.ui.statistics.StatisticsTestTags
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarTestTags
import kotlin.test.assertTrue
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The Analysis tab: its view selection, range, reminder table and calendar day events. */
class StatisticsRobot(private val ui: ComposeUi) {

    private val screen get() = ui.scope(ScreenTestTags.STATISTICS)
    private val table get() = screen.scope(SortableTableTestTags.TABLE)

    fun selectView(view: StatisticFragment) = screen.click(hasTestTag(StatisticsTestTags.viewChip(view)))

    fun selectRange(days: Int) {
        val labelRes = ANALYSIS_RANGES.first { it.second == days }.first
        screen.click(hasTestTag(StatisticsTestTags.RANGE_DROPDOWN))
        ui.scope(StatisticsTestTags.RANGE_MENU).click(hasText(ui.getString(labelRes)))
    }

    fun sortByColumn(@StringRes titleRes: Int) =
        table.scope(SortableTableTestTags.HEADER_ROW).click(hasText(ui.getString(titleRes)))

    fun filter(query: String) {
        screen.node(hasTestTag(StatisticsTestTags.TABLE_FILTER)).performTextReplacement(query)
        screen.waitForIdle()
    }

    fun clearFilter() = screen.scope(StatisticsTestTags.TABLE_FILTER)
        .click(hasContentDescription(ui.getString(CoreUiR.string.cancel)))

    fun assertTableContains(text: String) {
        table.await { table.exists(hasText(text, substring = true)) }
    }

    fun assertCalendarDayEventsContain(substring: String) {
        screen.await { screen.textsUnder(DAY_EVENTS).any { it.contains(substring) } }
    }

    fun assertNoCalendarDayEventContains(substring: String) {
        screen.node(DAY_EVENTS).assertExists()
        screen.waitForIdle()
        val texts = screen.textsUnder(DAY_EVENTS)
        assertTrue(texts.none { it.contains(substring) }, "A calendar day event contains '$substring': $texts")
    }

    private companion object {
        val DAY_EVENTS = hasTestTag(CalendarTestTags.DAY_EVENTS)
    }
}
