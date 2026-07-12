package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.domain.model.ScheduledReminder

class ScheduledReminderActions(
    val scheduledReminder: ScheduledReminder
) : Actions {

    private val isStockEvent = scheduledReminder.reminder.isOutOfStockOrExpirationReminder

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            visibleButtons.add(Button.ACKNOWLEDGED)
            visibleButtons.add(Button.RESCHEDULE)
        } else {
            visibleButtons.add(Button.TAKEN)
            visibleButtons.add(Button.SKIPPED)
            visibleButtons.add(Button.RESCHEDULE)
        }
    }

    override suspend fun buttonClicked(visitor: ActionsVisitor) {
        visitor.visit(scheduledReminder)
    }
}
