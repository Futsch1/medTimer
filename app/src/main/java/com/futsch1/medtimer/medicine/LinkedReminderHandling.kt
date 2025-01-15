package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.test.espresso.IdlingRegistry
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate

class LinkedReminderHandling(
    val reminder: Reminder,
    val medicineViewModel: MedicineViewModel
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
            medicineViewModel.insertReminder(linkedReminder)
            fragmentActivity.supportFragmentManager.popBackStack()
        }
    }

    fun deleteReminder(context: Context, thread: HandlerThread, postMainAction: () -> Unit) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder, {
            IdlingRegistry.getInstance().registerLooperAsIdlingResource(thread.looper)
            val threadHandler = Handler(thread.looper)
            threadHandler.post {
                internalDelete(reminder)
                Handler(Looper.getMainLooper()).post(postMainAction)
                IdlingRegistry.getInstance().unregisterLooperAsIdlingResource(thread.looper)
            }
        }, {})


    }

    private fun internalDelete(
        reminder: Reminder
    ) {
        val reminders: List<Reminder> = medicineViewModel.getLinkedReminders(reminder.reminderId)
        for (r in reminders) {
            internalDelete(r)
        }

        medicineViewModel.deleteReminder(reminder.reminderId)
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
                total += if (r.reminderType == Reminder.ReminderType.LINKED) {
                    getTotalTimeInMinutes(r, reminders)
                } else {
                    r.timeInMinutes
                }
            }
        }
        return total
    }

}