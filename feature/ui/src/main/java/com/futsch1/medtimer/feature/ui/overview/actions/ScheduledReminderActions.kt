package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.feature.ui.overview.model.SimulatedReminderEvent

class ScheduledReminderActions(
    val simulatedReminderEvent: SimulatedReminderEvent
) : Actions {

    private val isStockEvent = simulatedReminderEvent.scheduledReminder.reminder.isOutOfStockOrExpirationReminder

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            visibleButtons.add(Button.ACKNOWLEDGED)
            visibleButtons.add(Button.RESCHEDULE)
        } else {
            visibleButtons.add(Button.TAKEN)
            if (!simulatedReminderEvent.cannotBeSkipped) {
                visibleButtons.add(Button.SKIPPED)
            }
            visibleButtons.add(Button.RESCHEDULE)
        }
    }

    override suspend fun buttonClicked(visitor: ActionsVisitor) {
        visitor.visit(simulatedReminderEvent.scheduledReminder)
    }
}
