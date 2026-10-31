package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.test.espresso.Espresso.pressBack
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags
import com.futsch1.medtimer.feature.ui.medicine.dialogs.NewReminderTestTags
import java.time.LocalTime
import kotlin.time.Duration

/**
 * The medicine editor: its name, its reminders and its stock settings.
 * Which reminder type card, and how far back it is to the editor, are implementation -
 * callers state what the medicine should end up with.
 */
class MedicineEditorRobot(
    private val ui: ComposeUi,
    private val menus: MenuRobot,
    private val preferences: PreferenceScreenRobot,
    private val pickers: MaterialPickers,
    private val iconPicker: IconColorPickerRobot,
) {

    fun assertTitle(expected: String) =
        ui.scope(com.futsch1.medtimer.core.ui.ScreenTestTags.TOP_APP_BAR).assertDisplayed(hasText(expected))

    fun assertReminderSetupGuidanceDisplayed() = editorScope().assertDisplayed(hasTestTag(EditMedicineTestTags.SETUP_ADD_REMINDER))

    fun assertReminderSetupGuidanceAbsent() = editorScope().assertAbsent(hasTestTag(EditMedicineTestTags.SETUP_ADD_REMINDER))

    fun openGettingStartedFromGuidance() = editorScope().click(hasText(ui.getString(R.string.setup_guidance_getting_started)))

    fun openAddReminderFromGuidance() = editorScope().click(hasTestTag(EditMedicineTestTags.SETUP_ADD_REMINDER))

    fun closeReminderTypeChooser() = pressBack()

    fun assertReminderTypeDisplayed(@StringRes type: Int) =
        dialogScope().assertDisplayed(hasText(ui.getString(type)))

    fun openReminderTypesHelp() {
        val helpButton = ui.scope(NewReminderTestTags.HELP_BUTTON)
        helpButton.awaitSelfExists()
        helpButton.clickSelf()
    }

    fun rename(name: String) = editValue(R.string.medicine_name, name)

    fun setAmount(amount: String) = editValue(R.string.dosage, amount)

    fun assertAmount(expected: String) {
        val editor = editorScope()
        editor.scrollUntilText(hasTestTag(EditMedicineTestTags.REMINDER_CARD), hasText(expected), expected)
        editor.assertDisplayed(hasText(expected))
    }

    fun selectIcon(position: Int) {
        editorScope().click(hasContentDescription(ui.getString(R.string.icon)))
        iconPicker.selectIcon(position)
    }

    private fun editValue(@StringRes label: Int, value: String) {
        editorScope().click(hasText(ui.getString(label)))
        val dialog = ui.scope(EditMedicineTestTags.EDIT_VALUE_DIALOG)
        dialog.replaceText(hasTestTag(EditMedicineTestTags.EDIT_VALUE_INPUT), value)
        dialog.click(hasText(ui.getString(R.string.ok)))
    }

    private fun editorScope() = ui.scope(com.futsch1.medtimer.core.ui.ScreenTestTags.MEDICINES)

    fun addReminder(amount: String, time: LocalTime? = null) {
        openType(R.string.time_based_reminder)
        setReminderInput(R.string.dosage, amount)
        time?.let { setTime(R.string.time, it) }
        create()
    }

    fun addHourlyIntervalReminder(amount: String, interval: Duration) {
        openType(R.string.continuous_interval_reminder)
        setReminderInput(R.string.dosage, amount)
        setIntervalHours(interval)
        create()
    }

    /** An interval reminder that only fires between [windowStart] and [windowEnd] each day. */
    fun addWindowedIntervalReminder(
        amount: String,
        windowStart: LocalTime,
        windowEnd: LocalTime,
        interval: Duration,
    ) {
        openType(R.string.windowed_interval_reminder)
        setReminderInput(R.string.dosage, amount)
        setTime(R.string.daily_start_time, windowStart)
        setTime(R.string.daily_end_time, windowEnd)
        setIntervalHours(interval)
        create()
    }

    fun addStockReminder(threshold: String) {
        openType(R.string.out_of_stock_reminder)
        setReminderInput(R.string.reminder_threshold, threshold)
        create()
    }

    /** A stock reminder that fires at [time] on every day the stock is below the threshold. */
    fun addDailyStockReminder(threshold: String, time: LocalTime) {
        openType(R.string.out_of_stock_reminder)
        setReminderInput(R.string.reminder_threshold, threshold)
        dialogScope().click(hasText(ui.getString(R.string.daily_below_threshold)))
        setTime(R.string.time, time)
        create()
    }

    /** An expiration reminder that fires at [time] each day; defaults to the dialog's own default time. */
    fun addExpirationReminder(daysBefore: String, time: LocalTime? = null) {
        openType(R.string.expiration_reminder)
        setReminderInput(R.string.remind_days_before_expiration, daysBefore)
        time?.let { setTime(R.string.time, it) }
        create()
    }

    fun openAddReminderDialog() = editorScope().click(hasTestTag(EditMedicineTestTags.ADD_REMINDER))

    fun selectReminderType(@StringRes type: Int) = dialogScope().click(hasText(ui.getString(type)))

    fun setReminderInput(@StringRes label: Int, value: String) {
        dialogScope().click(hasText(ui.getString(label)))
        val editor = ui.scope(EditMedicineTestTags.EDIT_VALUE_DIALOG)
        editor.replaceText(hasTestTag(EditMedicineTestTags.EDIT_VALUE_INPUT), value)
        editor.click(hasText(ui.getString(R.string.ok)))
    }

    fun clickCreateReminder() = dialogScope().click(hasText(ui.getString(R.string.create_reminder)))

    fun assertInvalidInputDisplayed() = dialogScope().assertDisplayed(hasText(ui.getString(R.string.invalid_input)))

    fun cancelReminderDialog() = dialogScope().click(hasText(ui.getString(R.string.cancel)))

    /** Opens the stock settings, runs [block] there and returns to the editor. */
    fun inStockSettings(block: () -> Unit) {
        menus.clickEditMedicineOption(R.string.medicine_stock_settings)
        block()
        pressBack()
    }

    /** Only the values given are written; an empty string clears the field. */
    fun setStock(amount: String? = null, refillSize: String? = null) {
        inStockSettings {
            amount?.let { preferences.setValue(R.string.remaining_stock, it) }
            refillSize?.let { preferences.setValue(R.string.refill_size, it) }
        }
    }

    fun refillNow() = inStockSettings { preferences.click(R.string.refill_now) }

    fun assertStockAmount(expected: String) =
        inStockSettings { preferences.assertSummary(R.string.remaining_stock, expected) }

    fun assertRefillSize(expected: String) =
        inStockSettings { preferences.assertSummary(R.string.refill_size, expected) }

    private fun openType(@StringRes type: Int) {
        openAddReminderDialog()
        selectReminderType(type)
    }

    private fun setTime(@StringRes label: Int, time: LocalTime) {
        dialogScope().click(hasText(ui.getString(label)))
        pickers.pickTime(time)
    }

    private fun setIntervalHours(interval: Duration) {
        dialogScope().click(hasText(ui.getString(R.string.interval)))
        com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalHours)
        com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo(
            com.futsch1.medtimer.feature.ui.R.id.editIntervalTime,
            interval.inWholeHours.toString()
        )
        com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard()
        com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn(android.R.id.button1)
    }

    private fun create() {
        clickCreateReminder()
        dialogScope().awaitGone()
    }

    private fun dialogScope() = ui.scope(NewReminderTestTags.DIALOG)
}
