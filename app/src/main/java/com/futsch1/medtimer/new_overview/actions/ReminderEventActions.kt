package com.futsch1.medtimer.new_overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.new_overview.OverviewReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor

class ReminderEventActions(event: OverviewReminderEvent, val view: View, popupWindow: PopupWindow) : ActionsBase(view, popupWindow) {
    init {
        takenButton.visibility = View.VISIBLE
        skippedButton.visibility = View.VISIBLE
        reRaiseButton.visibility = View.VISIBLE
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
    }

    private fun processTakenOrSkipped(reminderEvent: ReminderEvent, taken: Boolean) {
        ReminderProcessor.requestActionIntent(view.context, reminderEvent.reminderEventId, taken)
    }

    private fun processDeleteReRaiseReminderEvent(reminderEvent: ReminderEvent) {
        DeleteHelper(view.context).deleteItem(R.string.delete_re_raise_event, {
            val medicineRepository = MedicineRepository(view.context.applicationContext as Application?)
            medicineRepository.deleteReminderEvent(reminderEvent.reminderEventId)
            ReminderProcessor.requestReschedule(view.context)
        }, {})
    }

}
