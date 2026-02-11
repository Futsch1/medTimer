package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver

open class ReminderEventActions(val event: OverviewReminderEvent, medicineRepository: MedicineRepository, fragmentActivity: FragmentActivity) :
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
            Button.RESCHEDULE -> Unit
            Button.ACKNOWLEDGED -> Unit
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
