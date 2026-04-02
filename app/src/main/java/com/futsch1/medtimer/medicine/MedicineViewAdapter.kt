package com.futsch1.medtimer.medicine

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.IdlingListAdapter
import com.futsch1.medtimer.helpers.SwipeHelper.MovedCallback
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.Collections


class MedicineViewAdapter @AssistedInject constructor(
    @Assisted private val activity: FragmentActivity,
    private val medicineRepository: MedicineRepository,
    private val medicineViewHolderFactory: MedicineViewHolder.Factory,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) :
    IdlingListAdapter<FullMedicineEntity, MedicineViewHolder>(MedicineDiff()), MovedCallback {

    @AssistedFactory
    interface Factory {
        fun create(activity: FragmentActivity): MedicineViewAdapter
    }

    init {
        setHasStableIds(true)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        return medicineViewHolderFactory.create(parent, activity)
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
        if (toPosition != -1) {
            val list = currentList.toList()

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

    class MedicineDiff : DiffUtil.ItemCallback<FullMedicineEntity>() {
        override fun areItemsTheSame(oldItem: FullMedicineEntity, newItem: FullMedicineEntity): Boolean {
            return oldItem.medicine.medicineId == newItem.medicine.medicineId
        }

        override fun areContentsTheSame(oldItem: FullMedicineEntity, newItem: FullMedicineEntity): Boolean {
            return oldItem == newItem
        }
    }
}