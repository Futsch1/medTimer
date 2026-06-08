package com.futsch1.medtimer.feature.ui.impl.overview.actions

import com.futsch1.medtimer.feature.ui.impl.overview.model.OverviewEvent

class MultipleActions(
    val events: List<OverviewEvent>
) : Actions {

    val allActions = events.map { ActionsFactory().createActions(it) }
    override suspend fun buttonClicked(visitor: ActionsVisitor) {
        for (action in allActions) {
            action?.buttonClicked(visitor)
        }
    }

    override val visibleButtons: MutableList<Button> = Button.entries.toMutableList()

    init {
        // Show only buttons that are visible in all actions
        for (action in allActions) {
            for (button in Button.entries) {
                if (action?.visibleButtons?.contains(button) == false) {
                    visibleButtons.remove(button)
                }
            }
        }
    }
}