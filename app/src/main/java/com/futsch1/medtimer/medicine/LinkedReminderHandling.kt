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
        val reminder = Reminder(reminder.medicineRelId)
        reminder.amount = amount
        reminder.createdTimestamp = Instant.now().toEpochMilli() / 1000
        reminder.cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
        reminder.instructions = ""
        reminder.linkedReminderId = reminder.reminderId

        TimePickerWrapper(
            fragmentActivity,
            R.string.add_linked_reminder,
            TimeFormat.CLOCK_24H
        ).show(0, 0) { minutes: Int ->
            reminder.timeInMinutes = minutes
            medicineViewModel.insertReminder(reminder)
        }
    }
}