package com.futsch1.medtimer.medicine.tags

import com.futsch1.medtimer.model.Tag
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.stream.Collectors

class TagFilterStore(
    private val persistentDataDataSource: PersistentDataDataSource,
    private var validTagIds: MutableStateFlow<Set<Int>?>
) {
    @Suppress("kotlin:S6291") // Preferences do not contain sensitive date
    var selectedTags =
        tagIdSetFromStringSet(persistentDataDataSource.data.value.filterTags)
        set(value) {
            persistentDataDataSource.setFilterTags(value.map { it.toString() }.toSet())
            field = value
            validTagIds.value = value
        }

    fun filterForDeletedTags(allTags: List<Tag>) {
        val validTagsIdsValue = validTagIds.value
        if (validTagsIdsValue != null) {
            selectedTags = validTagsIdsValue
        }
        selectedTags = selectedTags.stream().filter { tagId ->
            allTags.stream().filter { tag -> tag.id == tagId }.count() > 0
        }.collect(Collectors.toSet())
        validTagIds.value = selectedTags
    }

    init {
        validTagIds.value = selectedTags
    }

    private fun tagIdSetFromStringSet(stringListOfIds: Set<String>?): Set<Int> {
        return stringListOfIds?.stream()?.map { it.toInt() }?.collect(Collectors.toSet())
            ?: emptySet()
    }
}