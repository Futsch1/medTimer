package com.futsch1.medtimer.medicine.tags

import android.content.Context
import androidx.lifecycle.MutableLiveData
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
            sharedPreferences.edit()
                .putStringSet("filterTags", value.map { it.toString() }.toSet()).apply()
            field = value
            validTagIds.value = value
        }

    init {
        validTagIds.value = selectedTags
    }

    private fun tagIdSetFromStringSet(stringListOfIds: Set<String>?): Set<Int> {
        return stringListOfIds?.stream()?.map { it.toInt() }?.collect(Collectors.toSet())
            ?: emptySet()
    }
}