package com.futsch1.medtimer.robots

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags
import java.time.LocalTime

/**
 * The medicine editor: its reminders and its stock settings.
 * Which overflow entry, which reminder type card,
 * and how far back it is to the editor are implementation - callers state what the medicine should end up with.
 */
class MedicineEditorRobot(
    private val ui: ComposeUi,
    private val preferences: PreferenceScreenRobot,
    private val pickers: MaterialPickers,
) {

    fun rename(name: String) {
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editMedicineName, name)
    }

    fun openAdvancedSettings() {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
    }

    fun addReminder(amount: String, time: LocalTime? = null) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, amount)
        closeKeyboard()

        if (time != null) {
            clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
            pickers.pickTime(time)
        }
        closeKeyboard()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    fun addIntervalReminder(amount: String, intervalMinutes: Int) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.continuousIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, amount)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalMinutes)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editIntervalTime, intervalMinutes.toString())

        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    fun addStockReminder(threshold: String) {
        openStockReminderCard(threshold)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    /** A stock reminder that fires at [time] on every day the stock is below the threshold. */
    fun addDailyStockReminder(threshold: String, time: LocalTime) {
        openStockReminderCard(threshold)
        clickOn(R.string.daily_below_threshold)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editReminderTime)
        pickers.pickTime(time)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    fun addExpirationReminder(daysBefore: String) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.expirationDateReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editExpirationDaysBefore, daysBefore)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)
    }

    private fun openStockReminderCard(threshold: String) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.stockReminderCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editStockThreshold, threshold)
    }

    /** Opens the stock settings, runs [block] there and returns to the editor. */
    fun inStockSettings(block: () -> Unit) {
        ui.clickTag(EditMedicineTestTags.OVERFLOW)
        ui.clickMenuItem(R.string.medicine_stock_settings)
        block()
        pressBack()
    }

    /** Only the values given are written; an empty string clears the field. */
    fun setStock(amount: String? = null, unit: String? = null, refillSize: String? = null) {
        inStockSettings {
            amount?.let { preferences.setValue(R.string.amount, it) }
            unit?.let { preferences.setValue(R.string.unit, it) }
            refillSize?.let { preferences.setValue(R.string.refill_size, it) }
        }
    }

    fun refillNow() {
        inStockSettings { preferences.click(R.string.refill_now) }
    }

    fun assertStockAmount(expected: String) {
        inStockSettings { preferences.assertSummary(R.string.amount, expected) }
    }

    fun assertRefillSize(expected: String) {
        inStockSettings { preferences.assertSummary(R.string.refill_size, expected) }
    }
}
