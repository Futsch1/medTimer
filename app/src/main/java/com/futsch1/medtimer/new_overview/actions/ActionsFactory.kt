package com.futsch1.medtimer.new_overview.actions

import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.new_overview.OverviewEvent
import com.futsch1.medtimer.new_overview.OverviewReminderEvent
import com.futsch1.medtimer.new_overview.OverviewScheduledReminderEvent
import kotlinx.coroutines.CoroutineScope

fun createActions(event: OverviewEvent, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope) {
    if (event is OverviewReminderEvent) {
        ReminderEventActions(event, view, popupWindow)
    } else if (event is OverviewScheduledReminderEvent) {
        ScheduledReminderActions(event, view, popupWindow, coroutineScope)
    }
}