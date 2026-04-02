package com.futsch1.medtimer.medicine

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.helpers.IdlingListAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject


class ReminderViewAdapter @AssistedInject constructor(
    @Assisted private val fragmentActivity: FragmentActivity,
    private val reminderViewHolderFactory: ReminderViewHolder.Factory
) : IdlingListAdapter<ReminderEntity, ReminderViewHolder>(ReminderDiff()) {
    @AssistedFactory
    interface Factory {
        fun create(fragmentActivity: FragmentActivity): ReminderViewAdapter
    }

    private var fullMedicine: FullMedicineEntity? = null

    init {
        setHasStableIds(true)
    }

    fun setMedicine(fullMedicine: FullMedicineEntity) {
        this.fullMedicine = fullMedicine
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return reminderViewHolderFactory.create(parent, fragmentActivity)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, fullMedicine!!)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).reminderId.toLong()
    }

    class ReminderDiff : DiffUtil.ItemCallback<ReminderEntity>() {
        override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem.reminderId == newItem.reminderId
        }

        override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem == newItem
        }
    }
}

