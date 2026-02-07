package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.OverviewEvent

class MultipleActions(val events: List<OverviewEvent>, fragmentActivity: FragmentActivity) : Actions {
    val allActions = events.map { createActions(it, fragmentActivity) }
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