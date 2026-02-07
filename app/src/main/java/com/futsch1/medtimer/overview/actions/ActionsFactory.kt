package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import kotlinx.coroutines.CoroutineScope

fun createActions(event: OverviewEvent, fragmentActivity: FragmentActivity): Actions? {
    val medicineRepository = MedicineRepository(fragmentActivity.application)
    return if (event is OverviewReminderEvent) {
        if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) {
            StockEventActions(event, medicineRepository, fragmentActivity)
        } else {
            ReminderEventActions(event, medicineRepository, fragmentActivity)
        }
    } else if (event is OverviewScheduledReminderEvent) {
        if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) {
            ScheduledStockReminderActions(event, medicineRepository, fragmentActivity)
        } else {
            ScheduledReminderActions(event, medicineRepository, fragmentActivity)
        }
    } else {
        null
    }
}

fun createActionsView(actions: Actions, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    return ActionsView(view, popupWindow, coroutineScope, actions).visible
}
