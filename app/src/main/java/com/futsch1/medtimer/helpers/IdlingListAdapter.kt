package com.futsch1.medtimer.helpers

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.futsch1.medtimer.helpers.IdlingResourcesPool.Companion.getInstance

abstract class IdlingListAdapter<T, VH : RecyclerView.ViewHolder?>
protected constructor(diffCallback: DiffUtil.ItemCallback<T?>, idlingResourceName: String = "IdlingListAdapter_$diffCallback") :
    ListAdapter<T?, VH?>(diffCallback) {
    private val idlingResource: SimpleIdlingResource = getInstance().getResource(idlingResourceName)

    init {
        idlingResource.setBusy()
    }

    override fun submitList(list: MutableList<T?>?) {
        super.submitList(list)
        idlingResource.setIdle()
    }

    override fun submitList(list: MutableList<T?>?, commitCallback: Runnable?) {
        super.submitList(list, commitCallback)
        idlingResource.setIdle()
    }
}