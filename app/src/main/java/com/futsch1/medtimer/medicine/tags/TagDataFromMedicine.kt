package com.futsch1.medtimer.medicine.tags

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.DeleteHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class TagDataFromMedicine @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    @Assisted private val medicineId: Int,
    private val tagRepository: TagRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) : TagDataProvider() {

    @AssistedFactory
    interface Factory {
        fun create(fragment: Fragment, medicineId: Int): TagDataFromMedicine
    }

    private var viewModel: MedicineWithTagsViewModel = ViewModelProvider(
        fragment,
    )[MedicineWithTagsViewModel::class.java]
    private var tagsAdapter: TagsAdapter = TagsAdapter({ it: TagWithState ->
        fragment.lifecycleScope.launch {
            if (it.isSelected) {
                viewModel.associateTag(medicineId, it.tag.tagId)
            } else {
                viewModel.disassociateTag(medicineId, it.tag.tagId)
            }
        }
    }, { it: TagWithState ->
        DeleteHelper.deleteItem(fragment.requireContext(), R.string.are_you_sure_delete_tag, {
            fragment.lifecycleScope.launch {
                tagRepository.delete(it.tag)
            }
        }, {})
    })

    init {
        val tagsWithStateCollector =
            TagWithStateCollector { tagsAdapter.submitList(it) }

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tags.collect {
                tagsWithStateCollector.tags = it
            }
        }
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getMedicineWithTags(medicineId).collect {
                tagsWithStateCollector.fullMedicine = it
            }
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
            val tagId = tagRepository.create(TagEntity(tagName))
            viewModel.associateTag(medicineId, tagId.toInt())
        }
    }
}
