package com.futsch1.medtimer.feature.ui.impl.overview.actions

import com.futsch1.medtimer.feature.ui.impl.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.impl.overview.model.PastReminderEvent
import com.futsch1.medtimer.feature.ui.impl.overview.model.SimulatedReminderEvent

class ActionsFactory(
) {
    fun createActions(event: OverviewEvent): Actions? {
        return when (event) {
            is PastReminderEvent -> ReminderEventActions(event)
            is SimulatedReminderEvent -> ScheduledReminderActions(event.scheduledReminder)
            else -> null
        }
    }
}