package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.google.android.material.timepicker.TimeFormat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class LinkedReminderHandling @AssistedInject constructor(
    @Assisted val reminder: Reminder,
    val medicineRepository: MedicineRepository,
    @Assisted val coroutineScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) val dispatcher: CoroutineDispatcher,
    val timePickerDialogFactory: TimePickerDialogFactory
) {
    @AssistedFactory
    interface Factory {
        fun create(reminder: Reminder, coroutineScope: CoroutineScope): LinkedReminderHandling
    }

    fun addLinkedReminder(fragmentActivity: FragmentActivity) {
        TextInputDialogBuilder(fragmentActivity).title(R.string.add_linked_reminder)
            .hint(R.string.create_reminder_dosage_hint).textSink { amount: String? ->
                this.createReminder(
                    fragmentActivity,
                    amount!!
                )
            }.show()
    }

    private fun createReminder(fragmentActivity: FragmentActivity, amount: String) {
        val linkedReminder = Reminder(reminder.medicineRelId).apply {
            this.amount = amount
            createdTimestamp = Instant.now().toEpochMilli() / 1000
            cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
            instructions = ""
            linkedReminderId = reminder.reminderId
        }

        timePickerDialogFactory.create(0, 0, R.string.linked_reminder_delay, TimeFormat.CLOCK_24H) { minutes: Int ->
            coroutineScope.launch {
                linkedReminder.timeInMinutes = minutes
                medicineRepository.insertReminder(linkedReminder)
                fragmentActivity.supportFragmentManager.popBackStack()
            }
        }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    fun deleteReminder(context: Context, postYesAction: () -> Unit, postNoAction: () -> Unit) {
        DeleteHelper.deleteItem(context, R.string.are_you_sure_delete_reminder, {
            coroutineScope.launch(dispatcher) {
                internalDelete(reminder)
                Handler(Looper.getMainLooper()).post(postYesAction)
            }
        }, { Handler(Looper.getMainLooper()).post(postNoAction) })
    }

    private suspend fun internalDelete(
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