package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.TextInputDialogBuilder
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.model.Reminder
import com.google.android.material.timepicker.TimeFormat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class LinkedReminderHandling @AssistedInject constructor(
    @Assisted val reminder: Reminder,
    private val reminderRepository: ReminderRepository,
    @Assisted private val coroutineScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    private val timePickerDialogFactory: TimePickerDialogFactory
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
        val linkedReminder = Reminder.default().copy(
            medicineRelId = reminder.medicineRelId,
            amount = amount,
            createdTime = Instant.now(),
            cycleStartDay = LocalDate.now().plusDays(1),
            instructions = "",
            linkedReminderId = reminder.id
        )

        timePickerDialogFactory.create(0, 0, R.string.linked_reminder_delay, TimeFormat.CLOCK_24H) { minutes: Int ->
            coroutineScope.launch {
                reminderRepository.create(linkedReminder.copy(time = LocalTime.ofSecondOfDay(minutes * 60L)))
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
            reminderRepository.getLinked(reminder.id)
        for (r in reminders) {
            internalDelete(r)
        }

        reminderRepository.delete(reminder.id)
    }
}

