package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class LinkedReminderHandling(
    val reminder: Reminder,
    val medicineRepository: MedicineRepository,
    val coroutineScope: CoroutineScope,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun addLinkedReminder(fragmentActivity: FragmentActivity) {
        DialogHelper(fragmentActivity).title(R.string.add_linked_reminder)
            .hint(R.string.create_reminder_dosage_hint).textSink { amount: String? ->
                this.createReminder(
                    fragmentActivity,
                    amount
                )
            }.show()
    }

    private fun createReminder(fragmentActivity: FragmentActivity, amount: String?) {
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
            medicineRepository.insertReminder(linkedReminder)
            fragmentActivity.supportFragmentManager.popBackStack()
        }
    }

    fun deleteReminder(context: Context, postYesAction: () -> Unit, postNoAction: () -> Unit) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder, {
            coroutineScope.launch(dispatcher) {
                internalDelete(reminder)
                Handler(Looper.getMainLooper()).post(postYesAction)
            }
        }, { Handler(Looper.getMainLooper()).post(postNoAction) })
    }

    private fun internalDelete(
        reminder: Reminder
    ) {
        val reminders: List<Reminder> =
            medicineRepository.getLinkedReminders(reminder.reminderId)
        for (r in reminders) {
            internalDelete(r)
        }

        medicineRepository.deleteReminder(reminder.reminderId)
    }
}

class LinkedReminderAlgorithms {
    fun sortRemindersList(reminders: List<Reminder>): List<Reminder> {
        return reminders.sortedBy { r -> getTotalTimeInMinutes(r, reminders) }
    }

    private fun getTotalTimeInMinutes(reminder: Reminder, reminders: List<Reminder>): Int {
        var total = if (reminder.reminderType != Reminder.ReminderType.WINDOWED_INTERVAL) reminder.timeInMinutes else reminder.intervalStartTimeOfDay
        for (r in reminders) {
            if (r.reminderId == reminder.linkedReminderId) {
                total += when (r.reminderType) {
                    Reminder.ReminderType.LINKED -> {
                        getTotalTimeInMinutes(r, reminders)
                    }

                    else -> {
                        r.timeInMinutes
                    }
                }
            }
        }
        return total
    }

}