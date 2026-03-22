package com.futsch1.medtimer.overview.actions

import com.futsch1.medtimer.overview.OverviewEvent

class MultipleActions(
    private val actionsFactory: ActionsFactory,
    val events: List<OverviewEvent>
) : Actions {
    val allActions = events.map { actionsFactory.createActions(it) }
    override suspend fun buttonClicked(button: Button) {
        for (action in allActions) {
            action?.buttonClicked(button)
        }
    }

    override var visibleButtons: MutableList<Button> = Button.entries.toMutableList()

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