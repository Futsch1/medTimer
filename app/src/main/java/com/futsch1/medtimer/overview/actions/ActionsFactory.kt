package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.OverviewEvent
import com.futsch1.medtimer.overview.OverviewReminderEvent
import com.futsch1.medtimer.overview.OverviewScheduledReminderEvent
import javax.inject.Inject

class ActionsFactory @Inject constructor(
    private val reminderEventActionsFactory: ReminderEventActions.Factory,
    private val scheduledReminderActionsFactory: ScheduledReminderActions.Factory
) {
    fun createActions(event: OverviewEvent, fragmentActivity: FragmentActivity): Actions? {
        return when (event) {
            is OverviewReminderEvent -> reminderEventActionsFactory.create(event, fragmentActivity)
            is OverviewScheduledReminderEvent -> scheduledReminderActionsFactory.create(event, fragmentActivity)
            else -> null
        }
    }
}