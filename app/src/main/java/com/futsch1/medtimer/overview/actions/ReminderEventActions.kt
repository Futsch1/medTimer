package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

open class ReminderEventActions(val event: OverviewReminderEvent, medicineRepository: MedicineRepository, val fragmentActivity: FragmentActivity) :
    ActionsBase(medicineRepository, fragmentActivity) {

    init {
        if (event.state != OverviewState.TAKEN) {
            visibleButtons.add(Button.TAKEN)
        }
        if (event.state != OverviewState.SKIPPED) {
            visibleButtons.add(Button.SKIPPED)
        }

        if (event.state != OverviewState.RAISED) {
            visibleButtons.add(Button.RERAISE)
            visibleButtons.add(Button.DELETE)
        } else {
            visibleButtons.add(Button.RESCHEDULE)
        }
        if (event.reminderEvent.reminderId == -1) {
            visibleButtons.remove(Button.RERAISE)
        }
    }

    override suspend fun buttonClicked(button: Button) {
        when (button) {
            Button.TAKEN -> processTakenOrSkipped(event.reminderEvent, true)
            Button.SKIPPED -> processTakenOrSkipped(event.reminderEvent, false)
            Button.RERAISE -> processDeleteReRaiseReminderEvent(event.reminderEvent)
            Button.DELETE -> processDeleteReminderEvent(event.reminderEvent)
            Button.RESCHEDULE -> processPostponeReminder(event.reminderEvent)
            Button.ACKNOWLEDGED -> Unit
        }
    }

    private fun processPostponeReminder(reminderEvent: ReminderEvent) {
        val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(reminderEvent.remindedTimestamp), ZoneId.systemDefault())
        TimeHelper.TimePickerWrapper(fragmentActivity)
            .show(localDateTime.hour, localDateTime.minute) { minutes ->
                val newReminderTime = TimeHelper.changeTimeStampMinutes(reminderEvent.remindedTimestamp, minutes)
                fragmentActivity.lifecycleScope.launch(ioCoroutineDispatcher) {
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    reminderNotificationData.remindInstant = Instant.ofEpochSecond(newReminderTime)
                    ReminderProcessorBroadcastReceiver.requestShowReminderNotification(context, reminderNotificationData)
                }
            }
    }

    private fun processTakenOrSkipped(reminderEvent: ReminderEvent, taken: Boolean) {
        ReminderProcessorBroadcastReceiver.requestReminderAction(context, null, reminderEvent, taken)
    }

    private fun processDeleteReRaiseReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper(context).deleteItem(R.string.delete_re_raise_event, {
            medicineRepository.deleteReminderEvent(reminderEvent)
            ReminderProcessorBroadcastReceiver.requestScheduleNextNotification(context)
        }, {})
    }

    protected fun processDeleteReminderEvent(reminderEvent: ReminderEvent) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder_event, {
            reminderEvent.status = ReminderEvent.ReminderStatus.DELETED
            medicineRepository.updateReminderEventFromMain(reminderEvent)
        }, {})
    }
}
