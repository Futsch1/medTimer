package com.futsch1.medtimer.medicine.tags

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import com.futsch1.medtimer.MedicineViewModel
import kotlinx.coroutines.launch

class TagDataFromPreferences(fragment: Fragment) : TagDataProvider() {
    private var viewModel: MedicineWithTagsViewModel = ViewModelProvider(
        fragment,
    )[MedicineWithTagsViewModel::class.java]
    private var medicineViewModel: MedicineViewModel = ViewModelProvider(
        fragment,
    )[MedicineViewModel::class.java]

    private var tagFilterStore = medicineViewModel.tagFilterStore

    private var tagsAdapter: TagsAdapter = TagsAdapter({ it: TagWithState ->
        if (it.isSelected) {
            tagFilterStore.selectedTags += it.tag.tagId
        } else {
            tagFilterStore.selectedTags -= it.tag.tagId
        }
    }, null)

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tags.collect { tagList ->
                tagsAdapter.submitList(
                    tagList.map { TagWithState(it, tagFilterStore.selectedTags.contains(it.tagId)) }
                )
            }
        }
    }

    override fun getAdapter(): ListAdapter<TagWithState, TagViewHolder> {
        return tagsAdapter
    }
}