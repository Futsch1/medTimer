package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.futsch1.medtimer.NavTestTags
import com.futsch1.medtimer.feature.ui.medicine.MedicineTestTags
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import kotlin.test.assertTrue

/** The medicine list: creating, opening, and asserting on its cards. */
class MedicinesRobot(private val ui: ComposeUi, private val queries: SemanticsQueries) {

    private val rule get() = ui.rule

    fun create(name: String) {
        showList()
        ui.clickTag(MedicineTestTags.ADD_MEDICINE)
        onView(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.medicineName))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        writeTo(com.futsch1.medtimer.feature.ui.R.id.medicineName, name)

        clickDialogPositiveButton()
    }

    /** A medicine detail screen may sit on the tab's back stack, and a tab tap no longer pops it. */
    fun showList() {
        ui.clickTag(NavTestTags.MEDICINES)
        repeat(3) {
            if (queries.exists(MedicineTestTags.ADD_MEDICINE)) return
            pressBack()
            rule.waitForIdle()
        }
    }

    fun count(): Int = queries.count(MedicineTestTags.MEDICINE_ITEM)

    fun clickItem(position: Int) {
        showList()
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { count() > position }
        rule.onAllNodesWithTag(MedicineTestTags.MEDICINE_ITEM)[
            queries.indicesTopToBottom(MedicineTestTags.MEDICINE_ITEM)[position]
        ].performClick()
        ui.settle()
    }

    /**
     * Drags the card at [from] onto the position of the card at [to]. A slow, many-step drag is
     * needed for Compose's reorderable pointer input to read the gesture as a drag rather than a tap.
     */
    fun dragItem(from: Int, to: Int) {
        val handles = dragHandleNodes()
        val distance = handles[to].second - handles[from].second
        // Straight down the handle column: a diagonal drag towards the card's centre leaves the
        // handle early and the reorder never starts.
        handleAt(handles[from].first).performTouchInput {
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(0f, distance / DRAG_STEPS)) }
            up()
        }
        rule.waitForIdle()
    }

    /** Indices into the semantics collection paired with their y position, in visual order. */
    private fun dragHandleNodes(): List<Pair<Int, Float>> =
        dragHandles()
            .fetchSemanticsNodes()
            .withIndex()
            .map { it.index to it.value.boundsInRoot.center.y }
            .sortedBy { it.second }

    private fun handleAt(index: Int): SemanticsNodeInteraction = dragHandles()[index]

    /**
     * Unmerged: the card is clickable, so it merges the handle's content description into itself and
     * the merged match is the whole card - a touch at its centre misses the handle entirely.
     */
    private fun dragHandles() = rule.onAllNodesWithContentDescription(
        ui.getString(com.futsch1.medtimer.core.ui.R.string.move_medicine),
        useUnmergedTree = true
    )

    fun clickNamed(name: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { names().any { it.startsWith(name) } }
        clickItem(names().indexOfFirst { it.startsWith(name) })
    }

    fun assertCount(expected: Int) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { count() == expected }
    }

    fun assertAtPosition(position: Int, expectedName: String) {
        // A reorder reaches the list through the database, so the new order can lag the gesture.
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { matchesAtPosition(position, expectedName) }
    }

    /** The list appends a reminder count, e.g. "Test (2)". */
    private fun matchesAtPosition(position: Int, expectedName: String): Boolean {
        val actual = names().getOrNull(position) ?: return false
        return actual == expectedName || actual.startsWith("$expectedName (")
    }

    fun assertNameContains(text: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) { names().any { it.contains(text) } }
    }

    fun assertNameNotContains(text: String) {
        rule.waitForIdle()
        val names = names()
        assertTrue(names.none { it.contains(text) }, "A medicine name contains '$text' but should not: $names")
    }

    fun assertListContains(text: String) {
        rule.waitUntil(SemanticsQueries.DEFAULT_TIMEOUT) {
            queries.textsUnder(MedicineTestTags.MEDICINE_ITEM).any { it.contains(text) }
        }
    }

    fun assertListContains(@StringRes textRes: Int) = assertListContains(ui.getString(textRes))

    fun assertListDoesNotContain(text: String) {
        rule.waitForIdle()
        val texts = queries.textsUnder(MedicineTestTags.MEDICINE_ITEM)
        assertTrue(texts.none { it.contains(text) }, "A medicine card contains '$text': $texts")
    }

    fun assertListDoesNotContain(@StringRes textRes: Int) = assertListDoesNotContain(ui.getString(textRes))

    private fun names(): List<String> = queries.textsUnder(MedicineTestTags.MEDICINE_NAME)

    private companion object {
        const val DRAG_STEPS = 10
    }
}
