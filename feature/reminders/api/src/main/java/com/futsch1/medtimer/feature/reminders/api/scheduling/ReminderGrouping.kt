package com.futsch1.medtimer.feature.reminders.api.scheduling

/**
 * Groups [items] by their reminder time, where "same time" means an identical epoch second.
 *
 * This is the single definition of a reminder group. The overview list and the same-time
 * selection of the overview share it, so that a group on screen always holds the same events
 * as the selection a long press produces. There is deliberately no tolerance window: a merged
 * notification records the anchor instant on every one of its members, so an exact match is
 * what keeps the group, the notification and the recorded history in agreement.
 *
 * [epochSecond] extracts the reminder time of an item, so that a caller holding a [Long] epoch
 * second passes it directly and a caller holding a [java.time.Instant] passes `it.epochSecond`.
 *
 * The input order is preserved: the groups come out in the order their time is first seen, and
 * each group keeps the relative order of its members. An empty input yields an empty list.
 */
fun <T> groupBySameTime(items: List<T>, epochSecond: (T) -> Long): List<List<T>> =
    items.groupBy(epochSecond).values.toList()
