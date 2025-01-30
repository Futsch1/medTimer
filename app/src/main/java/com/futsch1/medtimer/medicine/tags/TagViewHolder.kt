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
        medicineId: Int,
        viewModel: MedicineWithTagsViewModel,
        selectable: Boolean
    ) {
        chip.apply {
            text = tagWithState.tag.name
            isChecked = tagWithState.isSelected
            gravity = Gravity.CENTER
            if (selectable) {
                setOnCheckedChangeListener { _, isChecked ->
                    // Update the tag's selected state
                    tagWithState.isSelected = isChecked
                    if (isChecked) {
                        viewModel.associateTag(medicineId, tagWithState.tag.tagId)
                    } else {
                        viewModel.disassociateTag(medicineId, tagWithState.tag.tagId)
                    }
                }
            } else {
                isCheckable = false
                isCloseIconVisible = false
                setOnClickListener {
                    (this.parent as View).performClick()
                }
                rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
                isFocusable = false
            }
            setOnCloseIconClickListener {
                viewModel.medicineRepository.deleteTag(tagWithState.tag)
            }
        }
    }
}