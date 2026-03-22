package com.futsch1.medtimer.overview.actions

import android.view.View
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class ActionsFactory @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val fragmentActivity: FragmentActivity,
    private val timePickerDialogFactory: TimePickerDialogFactory
) {
    fun createActions(event: OverviewEvent): Actions? {
        return when (event) {
            is OverviewReminderEvent if (event.reminderEvent.isOutOfStockOrExpirationOrRefillReminder) -> StockEventActions(
                event,
                medicineRepository,
                fragmentActivity,
                timePickerDialogFactory
            )

            is OverviewReminderEvent -> ReminderEventActions(event, medicineRepository, fragmentActivity, timePickerDialogFactory)
            is OverviewScheduledReminderEvent if (event.scheduledReminder.reminder.isOutOfStockOrExpirationReminder) -> ScheduledStockReminderActions(
                event,
                medicineRepository,
                fragmentActivity,
                timePickerDialogFactory
            )

            is OverviewScheduledReminderEvent -> ScheduledReminderActions(event, medicineRepository, fragmentActivity, timePickerDialogFactory)
            else -> null
        }
    }
}

fun createActionsView(actions: Actions, view: View, popupWindow: PopupWindow, coroutineScope: CoroutineScope): Boolean {
    return ActionsView(view, popupWindow, coroutineScope, actions).visible
}

