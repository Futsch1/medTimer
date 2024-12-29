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
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.google.gson.Gson


class MedicineEntityInterface : DatabaseEntityEditFragment.EntityInterface<Medicine> {
    override fun getEntity(medicineViewModel: MedicineViewModel, id: Int): Medicine? {
        return medicineViewModel.getMedicine(id)
    }

    override fun updateEntity(medicineViewModel: MedicineViewModel, entity: Medicine) {
        medicineViewModel.updateMedicine(entity)
    }
}

class ReminderEntityInterface : DatabaseEntityEditFragment.EntityInterface<Reminder> {
    override fun getEntity(medicineViewModel: MedicineViewModel, id: Int): Reminder? {
        return medicineViewModel.getReminder(id)
    }

    override fun updateEntity(medicineViewModel: MedicineViewModel, entity: Reminder) {
        medicineViewModel.updateReminder(entity)
    }
}

class ReminderEventEntityInterface : DatabaseEntityEditFragment.EntityInterface<ReminderEvent> {
    override fun getEntity(medicineViewModel: MedicineViewModel, id: Int): ReminderEvent? {
        return medicineViewModel.getReminderEvent(id)
    }

    override fun updateEntity(medicineViewModel: MedicineViewModel, entity: ReminderEvent) {
        medicineViewModel.updateReminderEvent(entity)
    }
}

abstract class DatabaseEntityEditFragment<T>(
    private val entityInterface: EntityInterface<T>,
    private val layoutId: Int,
    val name: String
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
    private var fragmentReady = false

    private val idlingResource = InitIdlingResource(name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.thread.start()
        medicineViewModel = MedicineViewModel(this.requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        idlingResource.resetInitialized()
        fragmentView = inflater.inflate(layoutId, container, false)

        // Do not enter fragment just yet, first fetch entity from database and setup UI
        postponeEnterTransition()

        setupMenu(fragmentView!!)

        val handler = Handler(thread.looper)
        handler.post {
            entity = entityInterface.getEntity(medicineViewModel, getEntityId())
            if (entity != null) {
                requireActivity().runOnUiThread {
                    // Signal that entity was loaded
                    if (onEntityLoaded(entity!!, fragmentView!!)) {
                        setFragmentReady()
                    }
                }
            }
        }

        return fragmentView!!
    }

    protected fun setFragmentReady() {
        // Only now allow getting data from fragment UI when it is closed
        fragmentReady = true
        idlingResource.setInitialized()
        // Now enter fragment
        startPostponedEnterTransition()
    }

    protected open fun setupMenu(fragmentView: View) {
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
        idlingResource.destroy()
    }

    override fun onStop() {
        super.onStop()
        if (fragmentReady) {
            val entityBefore = Gson().toJson(entity)
            fillEntityData(entity!! as T, fragmentView!!)
            if (entityBefore != Gson().toJson(entity)) {
                entityInterface.updateEntity(medicineViewModel, entity!!)
            }
        }
    }

    abstract fun onEntityLoaded(entity: T, fragmentView: View): Boolean
    abstract fun fillEntityData(entity: T, fragmentView: View)
    abstract fun getEntityId(): Int
}