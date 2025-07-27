package com.futsch1.medtimer.new_overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.new_overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.ReminderWork
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
    private val mainCoroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ActionsBase(view, popupWindow) {
    init {
        takenButton.visibility = View.VISIBLE
        skippedButton.visibility = View.VISIBLE
        reRaiseButton.visibility = View.GONE

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
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder, taken: Boolean) {
        // Perform database operations on the IO dispatcher
        withContext(ioCoroutineDispatcher) {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application) // Ensure Application context is not null
            val reminderEvent = ReminderWork.buildReminderEvent(
                scheduledReminder.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                scheduledReminder.medicine, scheduledReminder.reminder, medicineRepository
            )

            if (reminderEvent != null) {
                val reminderEventId = medicineRepository.insertReminderEvent(reminderEvent)
                // Switch back to the Main dispatcher to send broadcast
                withContext(mainCoroutineDispatcher) {
                    view.context.sendBroadcast(
                        if (taken)
                            ReminderProcessor.getTakenActionIntent(view.context, reminderEventId.toInt())
                        else
                            ReminderProcessor.getSkippedActionIntent(view.context, reminderEventId.toInt()),
                        "com.futsch1.medtimer.NOTIFICATION_PROCESSED"
                    )
                }
            }
        }
    }
}
