package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.component.SortableTableTestTags
import com.futsch1.medtimer.feature.ui.statistics.StatisticsTestTags
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarTestTags
import kotlin.test.assertTrue

/** The Analysis tab: its view selection, range, reminder table and calendar day events. */
class StatisticsRobot(private val ui: ComposeUi, private val queries: SemanticsQueries) {

    private val rule get() = ui.rule

    fun selectView(view: StatisticFragment) = ui.clickTag(StatisticsTestTags.viewChip(view))

    fun selectRange(days: Int) {
        ui.clickTag(StatisticsTestTags.RANGE_DROPDOWN)
        ui.clickTag(StatisticsTestTags.rangeOption(days))
    }

    fun sortByColumn(@StringRes titleRes: Int) = ui.clickTag(SortableTableTestTags.header(ui.getString(titleRes)))

    fun filter(query: String) {
        rule.onNodeWithTag(StatisticsTestTags.TABLE_FILTER).performTextReplacement(query)
        rule.waitForIdle()
    }

    fun assertTableContains(text: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertCalendarDayEventsContain(substring: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            queries.textsUnder(CalendarTestTags.DAY_EVENTS).any { it.contains(substring) }
        }
    }

    fun assertNoCalendarDayEventContains(substring: String) {
        rule.waitForIdle()
        val texts = queries.textsUnder(CalendarTestTags.DAY_EVENTS)
        assertTrue(texts.none { it.contains(substring) }, "A calendar day event contains '$substring': $texts")
    }
}
