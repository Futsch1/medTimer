package com.futsch1.medtimer.robots

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.test.espresso.Espresso.pressBack
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags

/** The reminders listed on a medicine, addressed by their position in the list. */
class ReminderListRobot(
    private val composeUi: ComposeUi,
    private val settings: ReminderSettingsRobot
) {
    private val screen get() = composeUi.scope(ScreenTestTags.MEDICINES)
    private val reminders get() = screen.scope(EditMedicineTestTags.REMINDER_LIST)
    private val cardTag = hasTestTag(EditMedicineTestTags.REMINDER_CARD)

    fun assertCount(expected: Int) {
        if (expected == 0) {
            reminders.scrollToIndex(0)
            screen.await { screen.count(cardTag) == 0 }
            return
        }
        for (position in 0 until expected) {
            val portraitIndex = 2 + position
            if (!reminders.scrollToIndex(portraitIndex)) check(reminders.scrollToIndex(1 + position))
            screen.await { screen.count(cardTag) > 0 }
        }
    }

    fun assertContains(text: String) = screen.assertDisplayed(hasText(text, substring = true))

    fun assertContainsTime(text: String) = screen.assertDisplayed(hasText(text, substring = true))

    fun inSettingsOf(position: Int, block: ReminderSettingsRobot.() -> Unit) {
        openSettings(position)
        settings.block()
        pressBack()
    }

    fun reopenSettings(position: Int) = inSettingsOf(0) {}

    fun addLinkedReminder(position: Int, amount: String? = null, hours: Int = 0, minutes: Int) {
        openSettings(position)
        settings.addLinkedReminder(amount, hours, minutes)
    }

    fun delete(position: Int) {
        openSettings(position)
        settings.delete()
    }

    fun duplicate(position: Int) {
        openSettings(position)
        settings.duplicate()
    }

    /** The Compose editor uses a dialog, so the former inline-field/keyboard placement assertion no longer applies. */
    fun assertLastAmountStaysAboveKeyboard() {
        val amount = hasText(composeUi.getString(R.string.dosage))
        screen.await { screen.count(amount) > 0 }
    }

    private fun openSettings(position: Int) {
        require(position >= 0) { "Reminder position cannot be negative" }
        val portraitIndex = 2 + position
        if (!reminders.scrollToIndex(portraitIndex)) check(reminders.scrollToIndex(1 + position))
        reminders.clickAt(hasText(composeUi.getString(R.string.advanced_settings)), 0)
    }
}
