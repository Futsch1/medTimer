package com.futsch1.medtimer.overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderNotificationWorker
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduledStockReminderActions(
    event: OverviewScheduledReminderEvent,
    view: View,
    popupWindow: PopupWindow,
    private val coroutineScope: CoroutineScope,
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScheduledReminderActions(event, view, popupWindow, coroutineScope) {
    init {
        hideDeleteAndSkipped()

        takenButton.setOnClickListener {
            // Launch a coroutine in the provided scope
            coroutineScope.launch {
                processFutureReminder(event.scheduledReminder)
            }
            popupWindow.dismiss()
        }
        takenButton.setText(R.string.acknowledged)

        reRaiseOrScheduleButton.setOnClickListener {
            scheduleReminder(event.scheduledReminder)
            popupWindow.dismiss()
        }
        reRaiseOrScheduleButton.setText(R.string.reschedule_reminder)
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder) {
        val reminderEvent = createReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderWorkerReceiver.requestStockReminderAcknowledged(view.context, reminderEvent)
    }

    private suspend fun createReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        return withContext(ioCoroutineDispatcher) {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application) // Ensure Application context is not null
            var reminderEvent = medicineRepository.getReminderEvent(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
            if (reminderEvent == null) {
                reminderEvent = ReminderNotificationWorker.buildReminderEvent(
                    reminderTimeStamp,
                    scheduledReminder.medicine, scheduledReminder.reminder, medicineRepository
                )
            }

            reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()

            return@withContext reminderEvent
        }
    }
}
