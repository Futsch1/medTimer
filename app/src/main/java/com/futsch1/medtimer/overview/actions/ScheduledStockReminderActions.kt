package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ScheduledStockReminderActions(
    event: OverviewScheduledReminderEvent,
    view: View,
    popupWindow: PopupWindow,
    private val coroutineScope: CoroutineScope
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

    private fun hideDeleteAndSkipped() {
        deleteButton.visibility = View.INVISIBLE
        skippedButton.visibility = View.INVISIBLE

        setAngle(anchorTakenButton, 70f)
        setAngle(anchorReraiseOrScheduleButton, 110f)
    }

    // Mark as suspend function as it performs async work and calls other suspend functions (withContext)
    private suspend fun processFutureReminder(scheduledReminder: ScheduledReminder) {
        val reminderEvent = createReminderEvent(scheduledReminder, scheduledReminder.timestamp.epochSecond)
        ReminderWorkerReceiver.requestStockReminderAcknowledged(view.context, reminderEvent)
    }
}
