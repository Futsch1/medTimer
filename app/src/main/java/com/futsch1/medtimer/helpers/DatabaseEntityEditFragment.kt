package com.futsch1.medtimer.helpers

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.database.Medicine


class MedicineEntityInterface : DatabaseEntityEditFragment.EntityInterface<Medicine> {
    override fun getEntity(medicineViewModel: MedicineViewModel, id: Int): Medicine? {
        return medicineViewModel.getMedicine(id)
    }

    override fun updateEntity(medicineViewModel: MedicineViewModel, entity: Medicine) {
        medicineViewModel.updateMedicine(entity)
    }
}

abstract class DatabaseEntityEditFragment<T>(
    private val entityInterface: EntityInterface<T>,
    private val layoutId: Int,
) :
    Fragment() {

    interface EntityInterface<T> {
        fun getEntity(medicineViewModel: MedicineViewModel, id: Int): T?
        fun updateEntity(medicineViewModel: MedicineViewModel, entity: T)
    }

    protected val thread = HandlerThread("DatabaseEntityEditFragment")
    private var entity: T? = null
    private var fragmentView: View? = null
    protected lateinit var medicineViewModel: MedicineViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.thread.start()
        medicineViewModel = MedicineViewModel(this.requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(layoutId, container, false)
        postponeEnterTransition()
        val handler = Handler(thread.looper)
        handler.post {
            entity = entityInterface.getEntity(medicineViewModel, getEntityId())
            if (entity != null) {
                requireActivity().runOnUiThread {
                    onEntityLoaded(entity!!, fragmentView!!)
                    startPostponedEnterTransition()
                }
            }
        }

        setupMenu(fragmentView)

        return fragmentView!!
    }

    protected open fun setupMenu(fragmentView: View?) {
        val optionsMenu = OptionsMenu(
            this.requireContext(),
            MedicineViewModel(requireActivity().application),
            this,
            fragmentView
        )
        requireActivity().addMenuProvider(optionsMenu, viewLifecycleOwner)
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.quitSafely()
    }

    override fun onStop() {
        super.onStop()
        if (entity != null && fragmentView != null) {
            fillEntityData(entity!! as T, fragmentView!!)
            entityInterface.updateEntity(medicineViewModel, entity!!)
        }
    }

    abstract fun onEntityLoaded(entity: T, fragmentView: View)
    abstract fun fillEntityData(entity: T, fragmentView: View)
    abstract fun getEntityId(): Int
}