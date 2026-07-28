package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags
import java.time.LocalDate
import kotlin.test.assertTrue

/** The Overview screen: its event list, day strip, filter chips and selection bar. */
class OverviewRobot(private val ui: ComposeUi, private val queries: SemanticsQueries) {

    private val rule get() = ui.rule

    fun eventCount(): Int = queries.count(OverviewTestTags.EVENT_CARD)

    /** Waits for the list to settle on [expected] rather than sampling it once. */
    fun assertEventCount(expected: Int) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { eventCount() == expected }
    }

    fun assertEventCountAtLeast(expected: Int) {
        rule.waitUntil(10_000) { eventCount() >= expected }
    }

    fun clickEventState(index: Int) {
        scrollToEvent(index)
        stateButton(index).performClick()
    }

    fun clickEvent(index: Int) {
        scrollToEvent(index)
        eventCard(index).performClick()
    }

    fun longClickEvent(index: Int) {
        scrollToEvent(index)
        eventCard(index).performTouchInput { longClick() }
    }

    fun assertEventState(index: Int, @StringRes stateRes: Int) {
        queries.awaitAtLeast(OverviewTestTags.EVENT_STATE_BUTTON, index + 1)
        stateButton(index).assertContentDescriptionEquals(ui.getString(stateRes))
    }

    fun assertEventTextContains(index: Int, substring: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            eventTexts().getOrNull(index)?.contains(substring) == true
        }
    }

    fun assertEventContains(substring: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            allEventTexts().any { it.contains(substring) }
        }
    }

    fun assertNoEventContains(substring: String) {
        rule.waitForIdle()
        val texts = allEventTexts()
        assertTrue(texts.none { it.contains(substring) }, "An Overview event contains '$substring': $texts")
    }

    fun take(substring: String) = actOnEventContaining(substring, com.futsch1.medtimer.core.ui.R.string.taken)

    fun skip(substring: String) = actOnEventContaining(substring, com.futsch1.medtimer.core.ui.R.string.skipped)

    /** The Overview filter chips carry the same descriptions as the selection actions, so scope to the bar. */
    fun clickSelectionAction(@StringRes textRes: Int) {
        rule.onNode(
            hasContentDescription(ui.getString(textRes)) and hasAnyAncestor(hasTestTag(OverviewTestTags.SELECTION_BAR))
        ).performClick()
        ui.settle()
    }

    fun assertSelectionCount(count: Int) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            queries.textsUnder(OverviewTestTags.SELECTION_BAR).any { it.contains(count.toString()) }
        }
    }

    fun toggleFilter(filter: OverviewFilter) = ui.clickTag(OverviewTestTags.filter(filter))

    fun assertDaySelected(date: LocalDate) {
        rule.onNodeWithTag(OverviewTestTags.day(date)).assertIsSelected()
    }

    /** [date] must be in the week currently shown; this does not page the strip. */
    fun clickDay(date: LocalDate) {
        val tag = OverviewTestTags.day(date)
        queries.awaitExists(tag)
        rule.onNodeWithTag(tag).performClick()
        ui.settle()
    }

    /** Scrolls the event list so the item at [index] is composed (the LazyColumn virtualizes it otherwise). */
    private fun scrollToEvent(index: Int) {
        queries.awaitAtLeast(OverviewTestTags.EVENT_STATE_BUTTON, index + 1)
        queries.scrollTo(OverviewTestTags.EVENT_LIST, index)
    }

    private fun actOnEventContaining(substring: String, @StringRes actionRes: Int) {
        queries.scrollUntilTextIn(
            OverviewTestTags.EVENT_LIST, OverviewTestTags.EVENT_CARD, OverviewTestTags.EVENT_TEXT, substring
        )

        val index = eventTexts().indexOfFirst { it.contains(substring) }
        assertTrue(index >= 0, "No Overview event contains '$substring': ${eventTexts()}")
        stateButton(index).performClick()
        ui.clickMenuItem(actionRes)
        stateButton(index).assertContentDescriptionEquals(ui.getString(actionRes))
    }

    /** Indices are visual, matching the event texts; the semantics tree is not necessarily in that order. */
    private fun stateButton(index: Int) = nodeAt(OverviewTestTags.EVENT_STATE_BUTTON, index)

    private fun eventCard(index: Int) = nodeAt(OverviewTestTags.EVENT_CARD, index)

    private fun nodeAt(tag: String, index: Int) =
        rule.onAllNodesWithTag(tag)[queries.indicesTopToBottom(tag)[index]]

    private fun eventTexts(): List<String> = queries.textsUnder(OverviewTestTags.EVENT_TEXT)

    private fun allEventTexts(): List<String> = queries.allTextsIn(
        OverviewTestTags.EVENT_LIST, OverviewTestTags.EVENT_CARD, OverviewTestTags.EVENT_TEXT
    )
}
