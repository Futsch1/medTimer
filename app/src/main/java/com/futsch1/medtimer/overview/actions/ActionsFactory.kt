package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.model.OverviewEvent
import com.futsch1.medtimer.overview.model.PastReminderEvent
import com.futsch1.medtimer.overview.model.ScheduledReminderEvent
import javax.inject.Inject

class ActionsFactory @Inject constructor(
    private val reminderEventActionsFactory: ReminderEventActions.Factory,
    private val scheduledReminderActionsFactory: ScheduledReminderActions.Factory
) {
    fun createActions(event: OverviewEvent, fragmentActivity: FragmentActivity): Actions? {
        return when (event) {
            is PastReminderEvent -> reminderEventActionsFactory.create(event, fragmentActivity)
            is ScheduledReminderEvent -> scheduledReminderActionsFactory.create(event, fragmentActivity)
            else -> null
        }
    }
}