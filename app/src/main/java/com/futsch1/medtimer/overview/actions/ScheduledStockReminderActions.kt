package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder

class ScheduledStockReminderActions(
    event: OverviewScheduledReminderEvent,
    medicineRepository: MedicineRepository,
    fragmentActivity: FragmentActivity
) : ScheduledReminderActions(event, medicineRepository, fragmentActivity) {
    init {
        visibleButtons.clear()
        visibleButtons.add(Button.ACKNOWLEDGED)
        visibleButtons.add(Button.RESCHEDULE)
    }

    override suspend fun buttonClicked(button: Button) {
        when (button) {
            Button.ACKNOWLEDGED -> processFutureReminder(event.scheduledReminder)
            Button.RESCHEDULE -> scheduleReminder(event.scheduledReminder)
            Button.TAKEN -> Unit
            Button.DELETE -> Unit
            Button.RERAISE -> Unit
            Button.SKIPPED -> Unit
        }
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder) {
        val reminderEvent = createReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderWorkerReceiver.requestStockReminderAcknowledged(context, reminderEvent)
    }
}
