package com.futsch1.medtimer.medicine.tags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.IdlingListAdapter

data class TagWithState(
    val tag: Tag,
    var isSelected: Boolean
)

class TagsAdapter(private val viewModel: MedicineWithTagsViewModel) :
    IdlingListAdapter<TagWithState, TagViewHolder>(TagsWithStateDiffCallback()) {

    private var selectable: Boolean = false

    fun selectable(selectable: Boolean) = apply { this.selectable = selectable }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tagWithState = getItem(position)
        return holder.bind(tagWithState, viewModel, selectable)
    }

    class TagsWithStateDiffCallback : DiffUtil.ItemCallback<TagWithState>() {
        override fun areItemsTheSame(oldItem: TagWithState, newItem: TagWithState): Boolean {
            return oldItem.tag.tagId == newItem.tag.tagId
        }

        override fun areContentsTheSame(oldItem: TagWithState, newItem: TagWithState): Boolean {
            return oldItem.tag.name == newItem.tag.name && oldItem.isSelected == newItem.isSelected
        }
    }
}