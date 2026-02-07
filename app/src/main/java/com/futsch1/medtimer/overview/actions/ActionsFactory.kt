package com.futsch1.medtimer.overview.actions

import android.app.Application
import android.view.View
import android.widget.PopupWindow
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import kotlinx.coroutines.CoroutineScope

fun createActions(event: OverviewEvent, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    val medicineRepository = MedicineRepository(view.context.applicationContext as Application?)
    val action = if (event is OverviewReminderEvent) {
        if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) {
            StockEventActions(event, medicineRepository, view.context as androidx.fragment.app.FragmentActivity)
        } else {
            ReminderEventActions(event, medicineRepository, view.context as androidx.fragment.app.FragmentActivity)
        }
    } else if (event is OverviewScheduledReminderEvent) {
        if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) {
            ScheduledStockReminderActions(event, medicineRepository, coroutineScope, view.context as androidx.fragment.app.FragmentActivity)
        } else {
            ScheduledReminderActions(event, medicineRepository, coroutineScope, view.context as androidx.fragment.app.FragmentActivity)
        }
    } else {
        null
    }
    return ActionsView(view, popupWindow, coroutineScope, action!!).visible
}
