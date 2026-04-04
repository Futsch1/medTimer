package com.futsch1.medtimer.medicine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderRepository
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
    @Assisted val reminder: ReminderEntity,
    private val reminderRepository: ReminderRepository,
    @Assisted private val coroutineScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    private val timePickerDialogFactory: TimePickerDialogFactory
) {
    @AssistedFactory
    interface Factory {
        fun create(reminder: ReminderEntity, coroutineScope: CoroutineScope): LinkedReminderHandling
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
        val linkedReminder = ReminderEntity(reminder.medicineRelId).apply {
            this.amount = amount
            createdTimestamp = Instant.now().toEpochMilli() / 1000
            cycleStartDay = LocalDate.now().plusDays(1).toEpochDay()
            instructions = ""
            linkedReminderId = reminder.reminderId
        }

        timePickerDialogFactory.create(0, 0, R.string.linked_reminder_delay, TimeFormat.CLOCK_24H) { minutes: Int ->
            coroutineScope.launch {
                linkedReminder.timeInMinutes = minutes
                reminderRepository.create(linkedReminder)
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
        reminder: ReminderEntity
    ) {
        val reminders: List<ReminderEntity> =
            reminderRepository.getLinked(reminder.reminderId)
        for (r in reminders) {
            internalDelete(r)
        }

        reminderRepository.delete(reminder.reminderId)
    }
}

