package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags
import java.time.LocalDate
import kotlin.test.assertTrue
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The Overview screen: its event list, day strip, filter chips, selection bar and action menu. */
class OverviewRobot(private val ui: ComposeUi) {

    private val screen get() = ui.scope(ScreenTestTags.OVERVIEW)
    private val list get() = screen.scope(OverviewTestTags.EVENT_LIST)
    private val selectionBar get() = screen.scope(OverviewTestTags.SELECTION_BAR)

    /** The arc menu renders in its own popup window, so it anchors on itself, not on the screen. */
    private val actionMenu get() = ui.scope(OverviewTestTags.ACTION_MENU)

    fun eventCount(): Int = list.count(EVENT_CARD)

    /** Waits for the list to settle on [expected] rather than sampling it once. */
    fun assertEventCount(expected: Int) {
        list.await { eventCount() == expected }
    }

    fun assertEventCountAtLeast(expected: Int) {
        list.await(LONG_TIMEOUT) { eventCount() >= expected }
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
        list.awaitAtLeast(EVENT_STATE_BUTTON, index + 1)
        stateButton(index).assertContentDescriptionEquals(ui.getString(stateRes))
    }

    fun assertEventTextContains(index: Int, substring: String) {
        list.await { eventTexts().getOrNull(index)?.contains(substring) == true }
    }

    fun assertEventContains(substring: String) {
        list.await { allEventTexts().any { it.contains(substring) } }
    }

    fun assertNoEventContains(substring: String) {
        list.self().assertExists()
        list.waitForIdle()
        val texts = allEventTexts()
        assertTrue(texts.none { it.contains(substring) }, "An Overview event contains '$substring': $texts")
    }

    fun take(substring: String) = actOnEventContaining(substring, CoreUiR.string.taken)

    fun skip(substring: String) = actOnEventContaining(substring, CoreUiR.string.skipped)

    /** Acts on the arc menu opened by an event's state button. */
    fun clickAction(@StringRes labelRes: Int) = actionMenu.click(hasText(ui.getString(labelRes)))

    /** Dismisses the arc action menu without choosing anything. */
    fun closeActionMenu() = pressBack()

    fun assertActionDisplayed(@StringRes labelRes: Int) =
        actionMenu.assertDisplayed(hasText(ui.getString(labelRes)))

    fun assertActionAbsent(@StringRes labelRes: Int) =
        actionMenu.assertAbsent(hasText(ui.getString(labelRes)))

    fun logManualDose() = screen.click(hasTestTag(OverviewTestTags.LOG_MANUAL_DOSE))

    fun previousWeek() = screen.click(description(CoreUiR.string.previous_week))

    fun nextWeek() = screen.click(description(CoreUiR.string.next_week))

    /** The Overview filter chips carry descriptions too, so the selection actions scope to the bar. */
    fun clickSelectionAction(@StringRes textRes: Int) = selectionBar.click(description(textRes))

    fun assertSelectionCount(count: Int) {
        screen.await { screen.textsUnder(SELECTION_BAR).any { it.contains(count.toString()) } }
    }

    fun toggleFilter(filter: OverviewFilter) = screen.click(hasTestTag(OverviewTestTags.filter(filter)))

    fun assertDaySelected(date: LocalDate) {
        screen.node(hasTestTag(OverviewTestTags.day(date))).assertIsSelected()
    }

    /** [date] must be in the week currently shown; this does not page the strip. */
    fun clickDay(date: LocalDate) = screen.click(hasTestTag(OverviewTestTags.day(date)))

    /**
     * Selects [date] wherever it sits relative to the week on screen:
     * which days the strip shows depend on the first day of the week and on what today happens to be,
     * so a neighboring day is not reliably in view.
     */
    fun selectDay(date: LocalDate) {
        val day = hasTestTag(OverviewTestTags.day(date))
        val page = if (date > LocalDate.now()) ::nextWeek else ::previousWeek
        var pagesLeft = PAGES_SEARCHED
        // Paging settles the strip, so a plain existence check after it is enough.
        while (!screen.exists(day) && pagesLeft-- > 0) {
            page()
        }
        screen.click(day)
    }

    /** Scrolls the event list so the item at [index] is composed (the LazyColumn virtualizes it otherwise). */
    private fun scrollToEvent(index: Int) {
        list.awaitAtLeast(EVENT_STATE_BUTTON, index + 1)
        list.scrollToIndex(index)
    }

    private fun actOnEventContaining(substring: String, @StringRes actionRes: Int) {
        list.scrollUntilText(EVENT_CARD, EVENT_TEXT, substring)

        val index = eventTexts().indexOfFirst { it.contains(substring) }
        assertTrue(index >= 0, "No Overview event contains '$substring': ${eventTexts()}")
        stateButton(index).performClick()
        clickAction(actionRes)
        stateButton(index).assertContentDescriptionEquals(ui.getString(actionRes))
    }

    /** Indices are visual, matching the event texts; the semantics tree is not necessarily in that order. */
    private fun stateButton(index: Int) = list.nodeAt(EVENT_STATE_BUTTON, index)

    private fun eventCard(index: Int) = list.nodeAt(EVENT_CARD, index)

    private fun eventTexts(): List<String> = list.textsUnder(EVENT_TEXT)

    private fun allEventTexts(): List<String> = list.allTexts(EVENT_CARD, EVENT_TEXT)

    private fun description(@StringRes textRes: Int) = hasContentDescription(ui.getString(textRes))

    private companion object {
        val EVENT_CARD = hasTestTag(OverviewTestTags.EVENT_CARD)
        val EVENT_STATE_BUTTON = hasTestTag(OverviewTestTags.EVENT_STATE_BUTTON)
        val EVENT_TEXT = hasTestTag(OverviewTestTags.EVENT_TEXT)
        val SELECTION_BAR = hasTestTag(OverviewTestTags.SELECTION_BAR)
        const val LONG_TIMEOUT = 10_000L

        /** A day the tests reach for is at most one week away, so one page in either direction covers it. */
        const val PAGES_SEARCHED = 1
    }
}
