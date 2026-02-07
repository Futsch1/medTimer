package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

open class ScheduledReminderActions(
    val event: OverviewScheduledReminderEvent,
    medicineRepository: MedicineRepository,
    private val coroutineScope: CoroutineScope,
    private val fragmentActivity: FragmentActivity
) : ActionsBase(medicineRepository, fragmentActivity) {
    init {
        visibleButtons.add(Button.TAKEN)
        visibleButtons.add(Button.SKIPPED)
        visibleButtons.add(Button.RESCHEDULE)
    }

    override suspend fun buttonClicked(button: Button) {
        when (button) {
            Button.TAKEN -> processFutureReminder(event.scheduledReminder, true)
            Button.SKIPPED -> processFutureReminder(event.scheduledReminder, false)
            Button.RESCHEDULE -> scheduleReminder(event.scheduledReminder)
            Button.DELETE -> Unit
            Button.RERAISE -> Unit
            Button.ACKNOWLEDGED -> Unit
        }
    }

    protected fun scheduleReminder(scheduledReminder: ScheduledReminder) {
        TimeHelper.TimePickerWrapper(fragmentActivity)
            .show(scheduledReminder.reminder.timeInMinutes / 60, scheduledReminder.reminder.timeInMinutes % 60) { minutes ->
                val reminderTimeStamp =
                    TimeHelper.instantFromDateAndMinutes(minutes, scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()).epochSecond
                coroutineScope.launch(ioCoroutineDispatcher) {
                    val reminderEvent = createReminderEvent(scheduledReminder, reminderTimeStamp)

                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    ReminderWorkerReceiver.requestShowReminderNotification(context, reminderNotificationData)
                }
            }
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder, taken: Boolean) {
        val reminderEvent = createReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderWorkerReceiver.requestReminderAction(context, scheduledReminder.reminder, reminderEvent, taken)
    }
}
