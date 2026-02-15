package com.futsch1.medtimer.overview

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.helpers.IdlingListAdapter

class RemindersViewAdapter(diffCallback: DiffUtil.ItemCallback<OverviewEvent>, val fragmentActivity: FragmentActivity) :
    IdlingListAdapter<OverviewEvent, ReminderViewHolder>(diffCallback),
    ReminderViewHolder.ClickDelegate {

    var selectionMode: Boolean = false
        set(value) {
            field = value
            if (!value) {
                clearSelection()
            }
        }
    private val selectedItems = mutableSetOf<Int>()
    var clickListener: ClickListener? = null

    init {
        setHasStableIds(true)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val holder = ReminderViewHolder.create(parent, fragmentActivity, this)
        holder.contentContainer.setOnLongClickListener {
            clickListener?.onItemLongClick(holder.layoutPosition)
            true
        }
        return holder
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, selectedItems.contains(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    fun selectSameTimeEvents(position: Int) {
        val selectedItem = getItem(position)
        val time = selectedItem.timestamp
        for ((index, event) in currentList.withIndex()) {
            if (event.timestamp == time) {
                toggleSelection(index)
            }
        }
    }

    fun clearSelection() {
        val selection = ArrayList(selectedItems)
        selectedItems.clear()
        for (i in selection) {
            notifyItemChanged(i)
        }
    }

    fun getSelectedCount(): Int {
        return selectedItems.size
    }

    fun getSelectedItems(): List<OverviewEvent> {
        if (selectedItems.size > itemCount) {
            selectAll()
        }
        return selectedItems.map { position -> getItem(position) }
    }

    override fun onItemClick(position: Int): Boolean {
        return if (selectionMode) {
            clickListener?.onItemClick(position)
            true
        } else {
            false
        }
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll((0 until itemCount).toList())
        notifyItemRangeChanged(0, itemCount)
    }

    class OverviewEventDiff : DiffUtil.ItemCallback<OverviewEvent>() {
        override fun areItemsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem == newItem
        }
    }

    interface ClickListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)
    }
}
