package com.futsch1.medtimer.overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.ReminderWorker
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class ScheduledReminderActions(
    event: OverviewScheduledReminderEvent,
    val view: View,
    popupWindow: PopupWindow,
    private val coroutineScope: CoroutineScope,
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ActionsBase(view, popupWindow) {
    init {
        hideDelete()

        takenButton.setOnClickListener {
            // Launch a coroutine in the provided scope
            coroutineScope.launch {
                processFutureReminder(event.scheduledReminder, true)
            }
            popupWindow.dismiss()
        }

        skippedButton.setOnClickListener {
            // Launch a coroutine in the provided scope
            coroutineScope.launch {
                processFutureReminder(event.scheduledReminder, false)
            }
            popupWindow.dismiss()
        }

        reRaiseOrScheduleButton.setOnClickListener {
            scheduleReminder(event.scheduledReminder)
            popupWindow.dismiss()
        }
        reRaiseOrScheduleButton.text = view.context.getString(R.string.reschedule_reminder)
    }

    private fun scheduleReminder(scheduledReminder: ScheduledReminder) {
        TimeHelper.TimePickerWrapper(view.context as androidx.fragment.app.FragmentActivity)
            .show(scheduledReminder.reminder.timeInMinutes / 60, scheduledReminder.reminder.timeInMinutes % 60) { minutes ->
                coroutineScope.launch {
                    val reminderTimeStamp =
                        TimeHelper.instantFromDateAndMinutes(minutes, scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()).epochSecond
                    val reminderEvent = createReminderEvent(scheduledReminder, reminderTimeStamp)
                    val reminderNotificationData = ReminderNotificationData.fromReminderEvent(reminderEvent)
                    ReminderProcessor.requestSchedule(view.context, reminderNotificationData)
                }
            }
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder, taken: Boolean) {
        val reminderEvent = createReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderProcessor.requestReminderAction(view.context, scheduledReminder.reminder, reminderEvent, taken)
    }

    private suspend fun createReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        return withContext(ioCoroutineDispatcher) {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application) // Ensure Application context is not null
            var reminderEvent = medicineRepository.getReminderEvent(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
            if (reminderEvent == null) {
                reminderEvent = ReminderWorker.buildReminderEvent(
                    reminderTimeStamp,
                    scheduledReminder.medicine, scheduledReminder.reminder, medicineRepository
                )
            }

            reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()

            return@withContext reminderEvent
        }
    }
}
