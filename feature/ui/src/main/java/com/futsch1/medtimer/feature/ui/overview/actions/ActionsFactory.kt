package com.futsch1.medtimer.feature.ui.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.overview.model.SimulatedReminderEvent
import javax.inject.Inject

class ActionsFactory @Inject constructor(
    private val reminderEventActionsFactory: ReminderEventActions.Factory,
    private val scheduledReminderActionsFactory: ScheduledReminderActions.Factory
) {
    fun createActions(event: OverviewEvent, fragmentActivity: FragmentActivity): Actions? {
        return when (event) {
            is PastReminderEvent -> reminderEventActionsFactory.create(event, fragmentActivity)
            is SimulatedReminderEvent -> scheduledReminderActionsFactory.create(event.scheduledReminder, fragmentActivity)
            else -> null
        }
    }
}