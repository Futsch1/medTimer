package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import com.futsch1.medtimer.reminders.ReminderContext
import kotlinx.coroutines.CoroutineScope

fun createActions(event: OverviewEvent, reminderContext: ReminderContext, fragmentActivity: FragmentActivity): Actions? {
    return if (event is OverviewReminderEvent) {
        if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) {
            StockEventActions(event, reminderContext, fragmentActivity)
        } else {
            ReminderEventActions(event, reminderContext, fragmentActivity)
        }
    } else if (event is OverviewScheduledReminderEvent) {
        if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) {
            ScheduledStockReminderActions(event, reminderContext, fragmentActivity)
        } else {
            ScheduledReminderActions(event, reminderContext, fragmentActivity)
        }
    } else {
        null
    }
}

fun createActionsView(actions: Actions, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    return ActionsView(view, popupWindow, coroutineScope, actions).visible
}
