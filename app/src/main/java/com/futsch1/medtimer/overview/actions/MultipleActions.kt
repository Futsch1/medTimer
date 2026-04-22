package com.futsch1.medtimer.overview.actions

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.overview.model.OverviewEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class MultipleActions @AssistedInject constructor(
    private val actionsFactory: ActionsFactory,
    @Assisted val events: List<OverviewEvent>,
    @Assisted fragmentActivity: FragmentActivity
) : Actions {

    @AssistedFactory
    interface Factory {
        fun create(events: List<OverviewEvent>, fragmentActivity: FragmentActivity): MultipleActions
    }

    val allActions = events.map { actionsFactory.createActions(it, fragmentActivity) }
    override suspend fun buttonClicked(button: Button) {
        for (action in allActions) {
            action?.buttonClicked(button)
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