package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver

class StockEventActions(
    event: OverviewReminderEvent,
    reminderContext: ReminderContext,
    fragmentActivity: FragmentActivity
) : ReminderEventActions(event, reminderContext, fragmentActivity) {
    init {
        visibleButtons.clear()
        if (event.state != OverviewState.RAISED) {
            visibleButtons.add(Button.DELETE)
        } else {
            visibleButtons.add(Button.ACKNOWLEDGED)
        }
    }

    override suspend fun buttonClicked(button: Button) {
        when (button) {
            Button.DELETE -> processDeleteReminderEvent(event.reminderEvent)
            Button.ACKNOWLEDGED -> ReminderProcessorBroadcastReceiver.requestStockReminderAcknowledged(reminderContext.context, event.reminderEvent)
            else -> Unit
        }

    }
}
