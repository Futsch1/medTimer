package com.futsch1.medtimer.medicine.tags

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ListAdapter
import com.futsch1.medtimer.database.Tag

class TagDataFromMedicine(fragment: Fragment, private val medicineId: Int) : TagDataProvider() {

    private var viewModel: MedicineWithTagsViewModel = ViewModelProvider(
        fragment,
    )[MedicineWithTagsViewModel::class.java]
    private var tagsAdapter: TagsAdapter = TagsAdapter({ it: TagWithState ->
        if (it.isSelected) {
            viewModel.associateTag(medicineId, it.tag.tagId)
        } else {
            viewModel.disassociateTag(medicineId, it.tag.tagId)
        }
    }, { it: TagWithState ->
        viewModel.medicineRepository.deleteTag(it.tag)
    })

    init {
        val tagsWithStateCollector =
            TagWithStateCollector { tagsAdapter.submitList(it) }

        viewModel.tags.observe(fragment) {
            tagsWithStateCollector.tags = it
        }
        viewModel.getMedicineWithTags(medicineId).observe(fragment) {
            tagsWithStateCollector.medicineWithTags = it
        }

    }

    override fun getAdapter(): ListAdapter<TagWithState, TagViewHolder> {
        return tagsAdapter
    }

    override fun canAddTag(): Boolean {
        return true
    }

    override fun addTag(tagName: String) {
        val tagId = viewModel.medicineRepository.insertTag(Tag(tagName))
        viewModel.associateTag(medicineId, tagId.toInt())
    }
}