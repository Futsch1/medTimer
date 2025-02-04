package com.futsch1.medtimer.medicine.tags

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import com.futsch1.medtimer.MedicineViewModel
import java.util.stream.Collectors

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
        viewModel.tags.observe(fragment) { tagList ->
            tagsAdapter.submitList(
                tagList.stream()
                    .map { TagWithState(it, tagFilterStore.selectedTags.contains(it.tagId)) }
                    .collect(Collectors.toList())
            )
        }
    }

    override fun getAdapter(): ListAdapter<TagWithState, TagViewHolder> {
        return tagsAdapter
    }
}