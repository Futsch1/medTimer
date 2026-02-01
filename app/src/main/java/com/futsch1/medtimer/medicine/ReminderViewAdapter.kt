package com.futsch1.medtimer.medicine

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.IdlingListAdapter
import com.futsch1.medtimer.medicine.ReminderViewHolder.Companion.create


class ReminderViewAdapter(private val fragmentActivity: FragmentActivity) : IdlingListAdapter<Reminder, ReminderViewHolder?>(ReminderDiff()) {
    private var fullMedicine: FullMedicine? = null

    init {
        setHasStableIds(true)
    }

    fun setMedicine(fullMedicine: FullMedicine) {
        this.fullMedicine = fullMedicine
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return create(parent, fragmentActivity)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, fullMedicine!!)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).reminderId.toLong()
    }

    class ReminderDiff : DiffUtil.ItemCallback<Reminder?>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.reminderId == newItem.reminderId
        }

        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}

