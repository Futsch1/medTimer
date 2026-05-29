package com.futsch1.medtimer.core.ui.filter

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.Tag
import javax.inject.Inject

/**
 * Filters reminder events by the active tag filter.
 *
 * The filter is the set of selected tag IDs (as persisted in `PersistentData.filterTags`) resolved
 * against the current [Tag] list — `ReminderEvent.tags` holds tag *names*, so IDs are mapped to names
 * first. An empty selection means "no filter" and all events pass.
 *
 * Lifted out of `MedicineViewModel.getFilteredEvents` so the Statistics screen can reuse tag filtering
 * without depending on another ViewModel.
 */
class TagEventFilter @Inject constructor() {
    fun filter(
        events: List<ReminderEvent>,
        selectedTagIds: Set<Int>,
        allTags: List<Tag>,
    ): List<ReminderEvent> {
        if (selectedTagIds.isEmpty()) {
            return events
        }
        val selectedTagNames = allTags.filter { it.id in selectedTagIds }.map { it.name }.toSet()
        return events.filter { event -> event.tags.any { it in selectedTagNames } }
    }
}
