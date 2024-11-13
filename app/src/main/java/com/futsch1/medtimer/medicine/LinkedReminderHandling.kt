package com.futsch1.medtimer.medicine

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate

class LinkedReminderHandling(
    private val fragmentActivity: FragmentActivity,
    val reminder: Reminder,
    val medicineViewModel: MedicineViewModel
) {
    fun addLinkedReminder() {
        DialogHelper.showTextInputDialog(
            fragmentActivity, R.string.add_linked_reminder, R.string.create_reminder_dosage_hint
        ) { amount: String? -> this.createReminder(amount) }
    }

    private fun createReminder(amount: String?) {
        val linkedReminder = Reminder(reminder.medicineRelId)
        linkedReminder.amount = amount
        linkedReminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        linkedReminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        linkedReminder.instructions = ""
        linkedReminder.linkedReminderId = reminder.reminderId

        TimePickerWrapper(
            fragmentActivity,
            R.string.linked_reminder_delay,
            TimeFormat.CLOCK_24H
        ).show(0, 0) { minutes: Int ->
            linkedReminder.timeInMinutes = minutes
            medicineViewModel.insertReminder(linkedReminder)
        }
    }
}

class LinkedReminderAlgorithms {
    fun sortRemindersList(reminders: List<Reminder>): List<Reminder> {
        return reminders.sortedBy { r -> getTotalTimeInMinutes(r, reminders) }
    }

    private fun getTotalTimeInMinutes(reminder: Reminder, reminders: List<Reminder>): Int {
        var total = reminder.timeInMinutes
        for (r in reminders) {
            if (r.reminderId == reminder.linkedReminderId) {
                total += if (r.linkedReminderId != 0) {
                    getTotalTimeInMinutes(r, reminders)
                } else {
                    r.timeInMinutes
                }
            }
        }
        return total
    }

}