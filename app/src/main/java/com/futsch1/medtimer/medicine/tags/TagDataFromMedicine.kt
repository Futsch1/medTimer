package com.futsch1.medtimer.medicine.tags

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.DeleteHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagDataFromMedicine(
    private val fragment: Fragment,
    private val medicineId: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TagDataProvider() {

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
        val deleteHelper = DeleteHelper(fragment.context)
        deleteHelper.deleteItem(R.string.are_you_sure_delete_tag, {
            viewModel.medicineRepository.deleteTag(it.tag)
        }, {})
    })

    init {
        val tagsWithStateCollector =
            TagWithStateCollector { tagsAdapter.submitList(it) }

        viewModel.tags.observe(fragment) {
            tagsWithStateCollector.tags = it
        }
        viewModel.getMedicineWithTags(medicineId).observe(fragment) {
            tagsWithStateCollector.fullMedicine = it
        }

    }

    override fun getAdapter(): ListAdapter<TagWithState, TagViewHolder> {
        return tagsAdapter
    }

    override fun canAddTag(): Boolean {
        return true
    }

    override fun addTag(tagName: String) {
        fragment.lifecycleScope.launch(dispatcher) {
            val tagId = viewModel.medicineRepository.insertTag(Tag(tagName))
            viewModel.associateTag(medicineId, tagId.toInt())
        }
    }
}