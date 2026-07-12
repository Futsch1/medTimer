package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.feature.ui.overview.model.OverviewState
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent

class ReminderEventActions(
    val event: PastReminderEvent
) : Actions {

    private val isStockEvent = event.reminderEvent.reminderType in setOf(
        ReminderType.OUT_OF_STOCK, ReminderType.EXPIRATION_DATE, ReminderType.REFILL
    )

    override val visibleButtons: MutableList<Button> = mutableListOf()

    init {
        if (isStockEvent) {
            if (event.state != OverviewState.RAISED) {
                visibleButtons.add(Button.DELETE)
            } else {
                visibleButtons.add(Button.ACKNOWLEDGED)
            }
        } else {
            if (event.state != OverviewState.TAKEN) {
                visibleButtons.add(Button.TAKEN)
            }
            if (event.state != OverviewState.SKIPPED) {
                visibleButtons.add(Button.SKIPPED)
            }
            if (event.state != OverviewState.RAISED && event.state != OverviewState.PENDING) {
                visibleButtons.add(Button.RERAISE)
                visibleButtons.add(Button.DELETE)
            } else {
                visibleButtons.add(Button.RESCHEDULE)
            }
            if (event.reminderEvent.reminderId == -1) {
                visibleButtons.remove(Button.RERAISE)
            }
        }
    }

    override suspend fun buttonClicked(visitor: ActionsVisitor) {
        visitor.visit(event)
    }
}
