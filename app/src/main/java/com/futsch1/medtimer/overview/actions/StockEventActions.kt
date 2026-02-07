package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewState

class StockEventActions(
    event: OverviewReminderEvent,
    medicineRepository: MedicineRepository,
    fragmentActivity: FragmentActivity
) : ReminderEventActions(event, medicineRepository, fragmentActivity) {
    init {
        visibleButtons.clear()
        if (event.state != OverviewState.RAISED) {
            visibleButtons.add(Button.DELETE)
        }
    }

    override suspend fun buttonClicked(button: Button) {
        when (button) {
            Button.DELETE -> processDeleteReminderEvent(event.reminderEvent)
            else -> Unit
        }

    }
}
