package com.futsch1.medtimer.new_overview

import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.helpers.IdlingListAdapter
import kotlinx.coroutines.CoroutineScope

class RemindersViewAdapter(diffCallback: DiffUtil.ItemCallback<OverviewEvent>, val coroutineScope: CoroutineScope) :
    IdlingListAdapter<OverviewEvent, ReminderViewHolder?>(diffCallback), Filterable {
    init {
        setHasStableIds(true)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.create(parent, coroutineScope)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        val positionEnum = when (position) {
            0 -> if (itemCount > 1) EventPosition.FIRST else EventPosition.ONLY
            itemCount - 1 -> EventPosition.LAST
            else -> EventPosition.MIDDLE
        }
        holder.bind(current, positionEnum)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getFilter(): Filter {
        return filter
    }

    class OverviewEventDiff : DiffUtil.ItemCallback<OverviewEvent>() {
        override fun areItemsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
