package com.futsch1.medtimer.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import org.hamcrest.Matchers

/** The reminders listed on a medicine, addressed by their position in the list. */
class ReminderListRobot(
    private val composeUi: ComposeUi,
    private val settings: ReminderSettingsRobot
) {

    fun assertCount(expected: Int) = assertListItemCount(REMINDER_LIST, expected)

    /** Position-independent: reminder order follows the reminder times, which move with the clock. */
    fun assertContains(text: String) {
        onView(
            Matchers.allOf(
                ViewMatchers.withSubstring(text),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(REMINDER_LIST)),
            )
            // Effective visibility rather than isDisplayed: a card further down the list is bound but
            // off-screen, and this asserts the reminder exists rather than where it sits.
        ).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    fun assertContainsTime(text: String) {
        onView(
            Matchers.allOf(
                ViewMatchers.withId(REMINDER_TIME),
                ViewMatchers.withSubstring(text),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(REMINDER_LIST)),
            )
        ).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    /** Opens the advanced settings of the reminder at [position], runs [block] and comes back out. */
    fun inSettingsOf(position: Int, block: ReminderSettingsRobot.() -> Unit) {
        openSettings(position)
        settings.block()
        pressBack()
    }

    /** Opening and closing the settings is what redraws a row whose reminder type changed. */
    fun reopenSettings(position: Int) = inSettingsOf(position) {}

    /** Adds a reminder linked to the one at [position]; the settings screen closes itself afterwards. */
    fun addLinkedReminder(position: Int, amount: String? = null, hours: Int = 0, minutes: Int) {
        openSettings(position)
        settings.addLinkedReminder(amount, hours, minutes)
    }

    fun delete(position: Int) {
        openSettings(position)
        // Deleting closes the settings screen itself, so there is nothing left to navigate back from.
        settings.delete()
    }

    fun duplicate(position: Int) {
        openSettings(position)
        settings.duplicate()
    }

    private fun openSettings(position: Int) {
        clickListItemChild(REMINDER_LIST, position, com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        composeUi.settle()
    }

    private companion object {
        val REMINDER_LIST = com.futsch1.medtimer.feature.ui.R.id.reminderList
        val REMINDER_TIME = com.futsch1.medtimer.feature.ui.R.id.editReminderTime
    }
}
