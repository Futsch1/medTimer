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
            // Taken asks for the amount of a variable-amount reminder, a step that a picked time would skip.
            if (!simulatedReminderEvent.scheduledReminder.reminder.variableAmount) {
                visibleButtons.add(Button.TAKEN_AT)
            }
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
