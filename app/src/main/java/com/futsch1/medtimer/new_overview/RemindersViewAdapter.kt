package com.futsch1.medtimer.new_overview

import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.helpers.IdlingListAdapter
import java.util.Locale

class RemindersViewAdapter(diffCallback: DiffUtil.ItemCallback<OverviewEvent>) :
    IdlingListAdapter<OverviewEvent, ReminderViewHolder?>(diffCallback), Filterable {
    private val filter: Filter = OverviewEventFilter()
    private var data: MutableList<OverviewEvent>? = null

    init {
        setHasStableIds(false)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.create(parent)
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
        return 0
    }

    override fun getFilter(): Filter {
        return filter
    }

    fun setData(data: MutableList<OverviewEvent>?) {
        this.data = data
    }

    class OverviewEventDiff : DiffUtil.ItemCallback<OverviewEvent>() {
        override fun areItemsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: OverviewEvent, newItem: OverviewEvent): Boolean {
            return oldItem == newItem
        }
    }

    private inner class OverviewEventFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filteredList: MutableList<OverviewEvent> = ArrayList()
            val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
            val showOnlyOpen = filterPattern.contains("o")
            if (data != null) {
                for (item in data) {
                    if (isVisible(item, showOnlyOpen)) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList
            results.count = filteredList.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            submitList(results.values as List<OverviewEvent>?)
        }

        private fun isVisible(item: OverviewEvent, showOnlyOpen: Boolean): Boolean {
            return !showOnlyOpen
        }
    }
}
