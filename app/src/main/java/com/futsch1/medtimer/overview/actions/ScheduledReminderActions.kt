package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.overview.model.ScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.time.ZoneId

class ScheduledReminderActions @AssistedInject constructor(
    @Assisted val event: ScheduledReminderEvent,
    @Assisted private val fragmentActivity: FragmentActivity,
    private val reminderEventCreator: ReminderEventCreator,
    private val timePickerDialogFactory: TimePickerDialogFactory
) : Actions {

    @AssistedFactory
    interface Factory {
        fun create(event: ScheduledReminderEvent, fragmentActivity: FragmentActivity): ScheduledReminderActions
    }

    private val isStockEvent = event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            visibleButtons.add(Button.ACKNOWLEDGED)
            visibleButtons.add(Button.RESCHEDULE)
        } else {
            visibleButtons.add(Button.TAKEN)
            visibleButtons.add(Button.SKIPPED)
            visibleButtons.add(Button.RESCHEDULE)
        }
    }

    override suspend fun buttonClicked(button: Button) {
        if (isStockEvent) {
            when (button) {
                Button.ACKNOWLEDGED -> processStockAcknowledged(event.scheduledReminder)
                Button.RESCHEDULE -> scheduleReminder(event.scheduledReminder)
                else -> Unit
            }
        } else {
            when (button) {
                Button.TAKEN -> processFutureReminder(event.scheduledReminder, true)
                Button.SKIPPED -> processFutureReminder(event.scheduledReminder, false)
                Button.RESCHEDULE -> scheduleReminder(event.scheduledReminder)
                else -> Unit
            }
        }
    }

    private fun scheduleReminder(scheduledReminder: ScheduledReminder) {
        timePickerDialogFactory
            .create(scheduledReminder.reminder.time.getLocalTime().hour, scheduledReminder.reminder.time.getLocalTime().minute) { minutes ->
                val reminderTimeStamp =
                    TimeHelper.instantFromDateAndMinutes(minutes, scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()).epochSecond
                fragmentActivity.lifecycleScope.launch {
                    val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, reminderTimeStamp)
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    ReminderProcessorBroadcastReceiver.requestShowReminderNotification(fragmentActivity, reminderNotificationData)
                }
            }.show(fragmentActivity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
    }

    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder, taken: Boolean) {
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderProcessorBroadcastReceiver.requestReminderAction(fragmentActivity, scheduledReminder.reminder, reminderEvent, taken)
    }

    private suspend fun processStockAcknowledged(scheduledReminder: ScheduledReminder) {
        val reminderEvent = reminderEventCreator.getOrCreateReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderProcessorBroadcastReceiver.requestStockReminderAcknowledged(fragmentActivity, reminderEvent)
    }
}
