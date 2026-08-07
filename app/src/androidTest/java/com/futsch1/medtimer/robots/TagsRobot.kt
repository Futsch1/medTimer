package com.futsch1.medtimer.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import org.hamcrest.Matchers
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * The tag dialog, reached either from a medicine or from the medicine list filter. Chips are scoped
 * to the dialog's own list, so a tag name that also appears on a medicine card cannot be matched.
 */
class TagsRobot(
    private val menus: MenuRobot,
    private val dialogs: DialogRobot,
) {

    /** Opens the medicine's tags, runs [block] and confirms. */
    fun inMedicineTags(block: TagsRobot.() -> Unit) {
        menus.clickEditMedicineOption(CoreUiR.string.tags)
        block()
        confirm()
    }

    /** Opens the medicine list's tag filter, runs [block], then confirms or dismisses. */
    fun inFilter(confirming: Boolean = true, block: TagsRobot.() -> Unit) {
        menus.openTagFilter()
        block()
        if (confirming) confirm() else pressBack()
    }

    fun add(name: String) {
        onView(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.addTag)).perform(ViewActions.click())
        dialogs.enterTextAndConfirm(name)
    }

    fun toggle(name: String) = chip(name).perform(ViewActions.click())

    /** Removes the chip at [position] and confirms the deletion. */
    fun remove(position: Int) {
        onView(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.tags))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(position, removeChip()))
        dialogs.confirm()
    }

    fun assertChecked(name: String) =
        chip(name).check(ViewAssertions.matches(ViewMatchers.isChecked()))

    fun assertNotChecked(name: String) =
        chip(name).check(ViewAssertions.matches(ViewMatchers.isNotChecked()))

    fun assertDisplayed(name: String) =
        chip(name).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    fun assertDoesNotExist(name: String) = chip(name).check(ViewAssertions.doesNotExist())

    private fun confirm() =
        onView(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.ok)).perform(ViewActions.click())

    private fun chip(name: String) = onView(
        Matchers.allOf(
            ViewMatchers.withText(name),
            ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.tags)),
        )
    )
}
