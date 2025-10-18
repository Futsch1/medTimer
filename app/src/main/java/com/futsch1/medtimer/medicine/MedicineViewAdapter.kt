package com.futsch1.medtimer.medicine

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.helpers.IdlingListAdapter
import com.futsch1.medtimer.helpers.SwipeHelper.MovedCallback
import com.futsch1.medtimer.medicine.MedicineViewHolder.Companion.create
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections


class MedicineViewAdapter(activity: FragmentActivity, medicineRepository: MedicineRepository, val dispatcher: CoroutineDispatcher = Dispatchers.IO) :
    IdlingListAdapter<FullMedicine, MedicineViewHolder?>(MedicineDiff()), MovedCallback {
    private val activity: FragmentActivity
    private val medicineRepository: MedicineRepository

    init {
        setHasStableIds(true)
        this.activity = activity
        this.medicineRepository = medicineRepository
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        return create(parent, activity)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).medicine.medicineId.toLong()
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        val list: MutableList<FullMedicine?> = ArrayList(currentList)
        if (toPosition != -1) {
            Collections.swap(list, fromPosition, toPosition)
            submitList(list)
        }
    }

    override fun onMoveCompleted(fromPosition: Int, toPosition: Int) {
        if (fromPosition != toPosition) {
            activity.lifecycleScope.launch(dispatcher) {
                medicineRepository.moveMedicine(fromPosition, toPosition)
            }
        }
    }

    class MedicineDiff : DiffUtil.ItemCallback<FullMedicine?>() {
        override fun areItemsTheSame(oldItem: FullMedicine, newItem: FullMedicine): Boolean {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId
        }

        override fun areContentsTheSame(oldItem: FullMedicine, newItem: FullMedicine): Boolean {
            return oldItem == newItem
        }
    }
}

