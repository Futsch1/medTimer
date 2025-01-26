package com.futsch1.medtimer.medicine.tags

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Tag
import com.google.android.material.chip.Chip

class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val chip: Chip = itemView.findViewById(R.id.tag)

    fun bind(tag: Tag, isSelected: Boolean) {
        chip.text = tag.name
        chip.isChecked = isSelected
    }
}