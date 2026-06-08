package com.futsch1.medtimer.feature.ui.impl.medicine.tags

import androidx.recyclerview.widget.ListAdapter

abstract class TagDataProvider {
    abstract fun getAdapter(): ListAdapter<TagWithState, TagViewHolder>
    open fun canAddTag(): Boolean {
        return false
    }

    open fun addTag(tagName: String) {
        // Empty per default
    }
}