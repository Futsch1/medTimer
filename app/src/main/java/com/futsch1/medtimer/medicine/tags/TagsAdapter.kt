package com.futsch1.medtimer.medicine.tags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.IdlingListAdapter

typealias TagCallback = ((TagWithState) -> Unit)

data class TagWithState(
    val tag: Tag,
    var isSelected: Boolean
)

class TagsAdapter(
    private val selectCallback: TagCallback?,
    private val deleteCallback: TagCallback?
) :
    IdlingListAdapter<TagWithState, TagViewHolder>(TagsWithStateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tagWithState = getItem(position)
        return holder.bind(tagWithState, selectCallback, deleteCallback)
    }

    class TagsWithStateDiffCallback : DiffUtil.ItemCallback<TagWithState>() {
        override fun areItemsTheSame(oldItem: TagWithState, newItem: TagWithState): Boolean {
            return oldItem.tag.tagId == newItem.tag.tagId
        }

        override fun areContentsTheSame(oldItem: TagWithState, newItem: TagWithState): Boolean {
            return oldItem.tag == newItem.tag && oldItem.isSelected == newItem.isSelected
        }
    }
}