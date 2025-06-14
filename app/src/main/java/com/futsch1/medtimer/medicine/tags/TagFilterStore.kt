package com.futsch1.medtimer.medicine.tags

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.futsch1.medtimer.database.Tag
import java.util.stream.Collectors

class TagFilterStore(
    context: Context,
    private var validTagIds: MutableLiveData<Set<Int>>
) {
    @Suppress("kotlin:S6291") // Preferences do not contain sensitive date
    private val sharedPreferences =
        context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)
    var selectedTags =
        tagIdSetFromStringSet(sharedPreferences.getStringSet("filterTags", emptySet()))
        set(value) {
            sharedPreferences.edit {
                putStringSet("filterTags", value.map { it.toString() }.toSet())
            }
            field = value
            validTagIds.value = value
        }

    fun filterForDeletedTags(allTags: List<Tag>) {
        if (validTagIds.value != null) {
            selectedTags = validTagIds.value!!
        }
        selectedTags = selectedTags.stream().filter { tagId ->
            allTags.stream().filter { tag -> tag.tagId == tagId }.count() > 0
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