package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import kotlinx.coroutines.CoroutineScope

fun createActions(event: OverviewEvent, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    return if (event is OverviewReminderEvent) {
        if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) {
            StockEventActions(event, view, popupWindow).visible
        } else {
            ReminderEventActions(event, view, popupWindow).visible
        }
    } else if (event is OverviewScheduledReminderEvent) {
        if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) {
            ScheduledStockReminderActions(event, view, popupWindow, coroutineScope).visible
        } else {
            ScheduledReminderActions(event, view, popupWindow, coroutineScope).visible
        }
    } else {
        false
    }
}