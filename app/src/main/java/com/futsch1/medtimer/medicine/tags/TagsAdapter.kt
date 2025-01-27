package com.futsch1.medtimer.medicine.tags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag

data class TagWithState(
    val tag: Tag,
    var isSelected: Boolean
)

class TagsAdapter(val tags: MutableList<TagWithState>, private val selectable: Boolean) :
    RecyclerView.Adapter<TagViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tag, parent, false)
        return TagViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tags.size
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        return holder.bind(tags[position], selectable)
    }
}