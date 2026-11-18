package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.feature.reminders.api.scheduling.groupBySameTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/** The events of one day that share one reminder time, shown as one card. */
data class OverviewGroup(val timestamp: Long, val events: ImmutableList<OverviewEvent>)

/** Taken or skipped: nothing is left to do for the event. */
val OverviewState.isResolved: Boolean get() = this == OverviewState.TAKEN || this == OverviewState.SKIPPED

/**
 * The order of the overview events: by reminder time, then by reminder, so that a row keeps its slot
 * in its group when it goes from scheduled to raised or resolved. The event id only breaks ties
 * between two events of the same reminder at the same time.
 */
internal val overviewEventOrder: Comparator<OverviewEvent> =
    compareBy<OverviewEvent> { it.timestamp }.thenBy { it.reminderId }.thenBy { it.id }

/**
 * Groups the already sorted [events] by their reminder time. Without [groupByReminderTime] every
 * event forms its own group.
 */
fun groupOverviewEvents(events: List<OverviewEvent>, groupByReminderTime: Boolean): ImmutableList<OverviewGroup> {
    val groups = if (groupByReminderTime) groupBySameTime(events) { it.timestamp } else events.map(::listOf)
    return groups.map { OverviewGroup(it.first().timestamp, it.toPersistentList()) }.toPersistentList()
}
