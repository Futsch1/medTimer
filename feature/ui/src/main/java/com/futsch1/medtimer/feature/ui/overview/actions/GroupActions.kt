package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.feature.ui.overview.model.OverviewEvent
import com.futsch1.medtimer.feature.ui.overview.model.OverviewGroup
import com.futsch1.medtimer.feature.ui.overview.model.isResolved

/**
 * What the header of one [OverviewGroup] offers. Both actions derive from [MultipleActions], so the
 * header never offers more than the selection bar offers for the same events.
 *
 * A group without an unresolved event offers nothing: every row still has its own state button.
 */
class GroupActions(group: OverviewGroup) {

    private val unresolved = group.events.filterNot { it.state.isResolved }

    /**
     * The events that `Confirm all` marks taken: the unresolved ones, so a part-resolved group
     * keeps the time of the doses already logged. Empty when the header shows no `Confirm all`,
     * which is the case for a one-event group, whose state button does the same job.
     */
    val confirmAllEvents: List<OverviewEvent> =
        unresolved.takeIf { group.events.size > 1 && it.isNotEmpty() && Button.TAKEN in MultipleActions(it).visibleButtons }
            .orEmpty()

    /**
     * The intersection of the member actions across the whole group, for the overflow menu. Empty
     * for a one-event group, which shows no header: its state button and long-press offer the same.
     */
    val menuButtons: List<Button> =
        if (group.events.size < 2 || unresolved.isEmpty()) emptyList() else MultipleActions(group.events).visibleButtons
}
