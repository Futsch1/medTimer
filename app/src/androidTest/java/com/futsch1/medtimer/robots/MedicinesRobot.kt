package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.feature.ui.medicine.MedicineTestTags
import kotlin.test.assertTrue
import com.futsch1.medtimer.core.ui.R as CoreUiR

/** The medicine list: creating, opening, reordering, and asserting on its cards. */
class MedicinesRobot(
    private val ui: ComposeUi,
    private val navigation: NavigationRobot,
    private val dialogs: DialogRobot,
) {

    private val screen get() = ui.scope(ScreenTestTags.MEDICINES)
    private val list get() = screen.scope(MedicineTestTags.MEDICINE_LIST)

    fun create(name: String) {
        showList()
        screen.click(ADD_MEDICINE)
        onView(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.medicineName))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        writeTo(com.futsch1.medtimer.feature.ui.R.id.medicineName, name)

        dialogs.confirm()
    }

    /** A medicine detail screen may sit on the tab's back stack, and a tab tap no longer pops it. */
    fun showList() {
        navigation.toMedicines()
        repeat(3) {
            if (screen.exists(ADD_MEDICINE)) return
            pressBack()
            screen.waitForIdle()
        }
    }

    fun count(): Int = list.count(MEDICINE_ITEM)

    fun clickItem(position: Int) {
        showList()
        list.await { count() > position }
        list.nodeAt(MEDICINE_ITEM, position).performClick()
        ui.settle()
    }

    /**
     * Drags the card at [from] onto the position of the card at [to]. A slow, many-step drag is
     * needed for Compose's reorderable pointer input to read the gesture as a drag rather than a tap.
     */
    fun dragItem(from: Int, to: Int) {
        val handles = list.boundsTopToBottom(dragHandle(), useUnmergedTree = true)
        val distance = handles[to].second - handles[from].second
        // Straight down the handle column: a diagonal drag towards the card's centre leaves the
        // handle early and the reorder never starts.
        handleAt(handles[from].first).performTouchInput {
            down(center)
            repeat(DRAG_STEPS) { moveBy(Offset(0f, distance / DRAG_STEPS)) }
            up()
        }
        list.waitForIdle()
    }

    private fun dragHandle() = hasContentDescription(ui.getString(CoreUiR.string.move_medicine))

    /**
     * Unmerged: the card is clickable, so it merges the handle's content description into itself and
     * the merged match is the whole card - a touch at its centre misses the handle entirely.
     */
    private fun handleAt(index: Int): SemanticsNodeInteraction =
        list.nodes(dragHandle(), useUnmergedTree = true)[index]

    fun clickNamed(name: String) {
        list.await { names().any { it.startsWith(name) } }
        clickItem(names().indexOfFirst { it.startsWith(name) })
    }

    fun assertCount(expected: Int) {
        showList()
        list.await { count() == expected }
    }

    fun assertAtPosition(position: Int, expectedName: String) {
        showList()
        // A reorder reaches the list through the database, so the new order can lag the gesture.
        list.await { matchesAtPosition(position, expectedName) }
    }

    /** The list appends a reminder count, e.g. "Test (2)". */
    private fun matchesAtPosition(position: Int, expectedName: String): Boolean {
        val actual = names().getOrNull(position) ?: return false
        return actual == expectedName || actual.startsWith("$expectedName (")
    }

    fun assertNameContains(text: String) {
        showList()
        list.await { names().any { it.contains(text) } }
    }

    fun assertNameNotContains(text: String) {
        showList()
        list.self().assertExists()
        list.waitForIdle()
        val names = names()
        assertTrue(names.none { it.contains(text) }, "A medicine name contains '$text' but should not: $names")
    }

    fun assertListContains(text: String) {
        showList()
        list.await { list.textsUnder(MEDICINE_ITEM).any { it.contains(text) } }
    }

    fun assertListContains(@StringRes textRes: Int) = assertListContains(ui.getString(textRes))

    fun assertListDoesNotContain(text: String) {
        showList()
        list.self().assertExists()
        list.waitForIdle()
        val texts = list.textsUnder(MEDICINE_ITEM)
        assertTrue(texts.none { it.contains(text) }, "A medicine card contains '$text': $texts")
    }

    fun assertListDoesNotContain(@StringRes textRes: Int) = assertListDoesNotContain(ui.getString(textRes))

    private fun names(): List<String> = list.textsUnder(MEDICINE_NAME)

    private companion object {
        val ADD_MEDICINE = hasTestTag(MedicineTestTags.ADD_MEDICINE)
        val MEDICINE_ITEM = hasTestTag(MedicineTestTags.MEDICINE_ITEM)
        val MEDICINE_NAME = hasTestTag(MedicineTestTags.MEDICINE_NAME)
        const val DRAG_STEPS = 10
    }
}
