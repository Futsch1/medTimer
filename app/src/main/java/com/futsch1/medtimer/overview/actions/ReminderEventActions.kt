package com.futsch1.medtimer.overview.actions

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData

class ReminderEventActions(event: OverviewReminderEvent, val view: View, popupWindow: PopupWindow) : ActionsBase(view, popupWindow) {
    init {
        if (event.state == OverviewState.RAISED) {
            hideDeleteAndReraise()
        }
        if (event.reminderEvent.reminderId == -1) {
            hideReraise()
        }
        takenButton.setOnClickListener {
            processTakenOrSkipped(event.reminderEvent, true)
            popupWindow.dismiss()
        }

        skippedButton.setOnClickListener {
            processTakenOrSkipped(event.reminderEvent, false)
            popupWindow.dismiss()
        }

        reRaiseButton.setOnClickListener {
            processDeleteReRaiseReminderEvent(event.reminderEvent)
            popupWindow.dismiss()
        }

        deleteButton.setOnClickListener {
            processDeleteReminderEvent(view.context, event.reminderEvent)
            popupWindow.dismiss()
        }
    }

    private fun hideReraise() {
        reRaiseButton.visibility = View.INVISIBLE

        setAngle(anchorTakenButton, 50f)
        setAngle(anchorSkippedButton, 90f)
        setAngle(anchorDeleteButton, 130f)
    }

    private fun processTakenOrSkipped(reminderEvent: ReminderEvent, taken: Boolean) {
        ReminderProcessor.requestActionIntent(view.context, ProcessedNotificationData(listOf(reminderEvent.reminderEventId)), taken)
    }

    private fun processDeleteReRaiseReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper(view.context).deleteItem(R.string.delete_re_raise_event, {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application?)
            medicineRepository.deleteReminderEvent(reminderEvent)
            ReminderProcessor.requestReschedule(view.context)
        }, {})
    }

    private fun processDeleteReminderEvent(context: Context?, reminderEvent: ReminderEvent) {
        val deleteHelper = DeleteHelper(context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder_event, {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application?)
            reminderEvent.status = ReminderEvent.ReminderStatus.DELETED
            medicineRepository.updateReminderEvent(reminderEvent)
        }, {})
    }


}
