package com.futsch1.medtimer.medicine.tags

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import java.util.stream.Collectors

class TagDataFromPreferences(fragment: Fragment) : TagDataProvider() {
    private var viewModel: MedicineWithTagsViewModel = ViewModelProvider(
        fragment,
    )[MedicineWithTagsViewModel::class.java]

    @Suppress("kotlin:S6291") // Preferences do not contain sensitive date
    private var sharedPreferences =
        fragment.requireActivity().getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)
    private var selectedTags =
        tagIdSetFromStringSet(sharedPreferences.getStringSet("filterTags", emptySet()))

    private var tagsAdapter: TagsAdapter = TagsAdapter({ it: TagWithState ->
        if (it.isSelected) {
            selectedTags += it.tag.tagId
        } else {
            selectedTags -= it.tag.tagId
        }
        updateSelectedTags()
    }, null)

    init {
        viewModel.tags.observe(fragment) { tagList ->
            tagsAdapter.submitList(
                tagList.stream().map { TagWithState(it, selectedTags.contains(it.tagId)) }
                    .collect(Collectors.toList())
            )
        }
    }

    private fun tagIdSetFromStringSet(stringListOfIds: Set<String>?): Set<Int> {
        return stringListOfIds?.stream()?.map { it.toInt() }?.collect(Collectors.toSet())
            ?: emptySet()
    }


    private fun updateSelectedTags() {
        // Store selected tags in preferences
        sharedPreferences.edit()
            .putStringSet("filterTags", selectedTags.map { it.toString() }.toSet()).apply()
    }

    override fun getAdapter(): ListAdapter<TagWithState, TagViewHolder> {
        return tagsAdapter
    }
}