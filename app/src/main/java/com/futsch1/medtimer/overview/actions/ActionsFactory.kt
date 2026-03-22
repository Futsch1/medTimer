package com.futsch1.medtimer.overview.actions

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class ActionsFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val fragmentActivity: FragmentActivity
) {
    fun createActions(event: OverviewEvent): Actions? {
        return if (event is OverviewReminderEvent) {
            if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) {
                StockEventActions(event, context, medicineRepository, fragmentActivity)
            } else {
                ReminderEventActions(event, context, medicineRepository, fragmentActivity)
            }
        } else if (event is OverviewScheduledReminderEvent) {
            if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) {
                ScheduledStockReminderActions(event, context, medicineRepository, fragmentActivity)
            } else {
                ScheduledReminderActions(event, context, medicineRepository, fragmentActivity)
            }
        } else {
            null
        }
    }
}

fun createActionsView(actions: Actions, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    return ActionsView(view, popupWindow, coroutineScope, actions).visible
}

