package com.futsch1.medtimer.medicine

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.helpers.IdlingListAdapter
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject


class ReminderViewAdapter @AssistedInject constructor(
    @Assisted private val fragmentActivity: FragmentActivity,
    private val reminderViewHolderFactory: ReminderViewHolder.Factory
) : IdlingListAdapter<Reminder, ReminderViewHolder>(ReminderDiff()) {
    @AssistedFactory
    interface Factory {
        fun create(fragmentActivity: FragmentActivity): ReminderViewAdapter
    }

    private lateinit var medicine: Medicine

    init {
        setHasStableIds(true)
    }

    fun setMedicine(medicine: Medicine) {
        this.medicine = medicine
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return reminderViewHolderFactory.create(parent, fragmentActivity)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, medicine)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    class ReminderDiff : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}

