package com.futsch1.medtimer.core.ui.filter

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.Tag
import javax.inject.Inject

// Lifted out of MedicineViewModel so the Statistics screen can reuse tag filtering without a ViewModel dependency.
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
