package com.futsch1.medtimer.medicine.tags

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.R
import com.google.android.material.chip.Chip

class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val chip: Chip = itemView.findViewById(R.id.tag)

    fun bind(
        tagWithState: TagWithState,
        selectCallback: TagCallback?,
        deleteCallback: TagCallback?
    ) {
        chip.apply {
            text = tagWithState.tag.name
            isChecked = tagWithState.isSelected
            gravity = Gravity.CENTER
            if (selectCallback != null) {
                setOnCheckedChangeListener { _, isChecked ->
                    tagWithState.isSelected = isChecked
                    selectCallback(tagWithState)
                }
            } else {
                isCheckable = false
                setOnClickListener {
                    (this.parent as View).performClick()
                }
                rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
                isFocusable = false
            }
            if (deleteCallback != null) {
                setOnCloseIconClickListener {
                    deleteCallback(tagWithState)
                }
            } else {
                isCloseIconVisible = false
            }
        }
    }
}