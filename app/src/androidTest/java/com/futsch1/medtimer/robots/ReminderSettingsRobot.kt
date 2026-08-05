package com.futsch1.medtimer.robots

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.test.espresso.Espresso.pressBack
import com.futsch1.medtimer.core.ui.ScreenTestTags
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderSettingsTestTags
import java.time.LocalTime
import java.util.Date
import com.futsch1.medtimer.core.ui.R as CoreUiR

/**
 * A reminder's advanced settings and the screens below it. Rows are named by what they set, so the
 * nesting - and how far back it is out again - stays out of the tests.
 */
class ReminderSettingsRobot(
    private val ui: ComposeUi,
    private val preferences: PreferenceScreenRobot,
    private val pickers: MaterialPickers,
    private val dialogs: DialogRobot,
) {

    fun toggleEnabled() = preferences.click(CoreUiR.string.reminder_enabled)

    fun toggleVariableAmount() = preferences.click(CoreUiR.string.variable_amount)

    fun toggleAutomaticallyTaken() = preferences.click(CoreUiR.string.automatically_taken)

    fun duplicate() = topBarAction(CoreUiR.string.duplicate)

    /** Deleting closes the settings screen, so this leaves the caller on the medicine editor. */
    fun delete() {
        topBarAction(CoreUiR.string.delete)
        dialogs.confirm()
    }

    /** Adding a linked reminder closes the settings screen, as [delete] does. */
    fun addLinkedReminder(amount: String? = null, hours: Int = 0, minutes: Int) {
        preferences.click(CoreUiR.string.add_linked_reminder)
        amount?.let { dialogs.enterText(it) }
        dialogs.confirm()
        pickers.pickDuration(hours, minutes)
    }

    fun setIntervalStart(date: Date, time: LocalTime) {
        preferences.click(CoreUiR.string.interval_start_time)
        pickers.pickDate(date)
        pickers.pickTime(time)
    }

    fun assertIntervalStart(expected: String) =
        preferences.assertSummary(CoreUiR.string.interval_start_time, expected)

    /** The start/end period screen. Its date rows only appear once the matching switch is on. */
    fun inPeriod(block: ReminderSettingsRobot.() -> Unit) = inScreen(CoreUiR.string.reminder_status, block)

    fun enablePeriodStart() = preferences.click(CoreUiR.string.period_start)

    fun enablePeriodEnd() = preferences.click(CoreUiR.string.period_end)

    fun setPeriodStartDate(date: Date) = pickDateFor(CoreUiR.string.start_date, date)

    fun setPeriodEndDate(date: Date) = pickDateFor(CoreUiR.string.end_date, date)

    fun inCycle(block: ReminderSettingsRobot.() -> Unit) = inScreen(CoreUiR.string.cycle_reminder, block)

    fun setCycleStartDate(date: Date) = pickDateFor(CoreUiR.string.cycle_start_date, date)

    fun setConsecutiveDays(days: Int) =
        preferences.setValue(CoreUiR.string.cycle_consecutive_days, days.toString())

    fun setPauseDays(days: Int) = preferences.setValue(CoreUiR.string.cycle_pause_days, days.toString())

    fun assertCycleStartDate(expected: String) =
        preferences.assertSummary(CoreUiR.string.cycle_start_date, expected)

    fun assertConsecutiveDays(expected: String) =
        preferences.assertSummary(CoreUiR.string.cycle_consecutive_days, expected)

    fun assertPauseDays(expected: String) =
        preferences.assertSummary(CoreUiR.string.cycle_pause_days, expected)

    /** The weekday picker, confirmed on the way out. */
    fun inWeekdays(block: DialogRobot.() -> Unit) = inDialog(CoreUiR.string.remind_on_weekdays, block)

    fun inDaysOfMonth(block: DialogRobot.() -> Unit) = inDialog(CoreUiR.string.remind_on_days_of_month, block)

    fun inDosingInstructions(block: ReminderSettingsRobot.() -> Unit) =
        inScreen(CoreUiR.string.dosing_instructions, block)

    fun useSampleInstruction(@StringRes instructionRes: Int) {
        preferences.click(CoreUiR.string.sample_instructions)
        dialogs.clickItem(instructionRes)
    }

    fun assertDosingInstructions(expected: String) =
        preferences.assertSummary(CoreUiR.string.dosing_instructions, expected)

    private fun inScreen(@StringRes titleRes: Int, block: ReminderSettingsRobot.() -> Unit) {
        preferences.click(titleRes)
        block()
        pressBack()
    }

    private fun inDialog(@StringRes titleRes: Int, block: DialogRobot.() -> Unit) {
        preferences.click(titleRes)
        dialogs.block()
        dialogs.confirm()
    }

    private fun pickDateFor(@StringRes titleRes: Int, date: Date) {
        preferences.click(titleRes)
        pickers.pickDate(date)
    }

    private fun topBarAction(@StringRes labelRes: Int) {
        ui.scope(ScreenTestTags.TOP_APP_BAR).click(hasContentDescription(ui.getString(CoreUiR.string.more_options)))
        ui.scope(AdvancedReminderSettingsTestTags.MENU).click(hasText(ui.getString(labelRes)))
    }
}
