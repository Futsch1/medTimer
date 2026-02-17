package com.futsch1.medtimer.helpers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.OptionsMenu
import com.futsch1.medtimer.database.FullMedicine
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FullMedicineEntityInterface : DatabaseEntityEditFragment.EntityInterface<FullMedicine> {
    override fun getEntity(medicineViewModel: MedicineViewModel, id: Int): FullMedicine? {
        return medicineViewModel.medicineRepository.getMedicine(id)
    }

    override fun updateEntity(medicineViewModel: MedicineViewModel, entity: FullMedicine) {
        medicineViewModel.medicineRepository.updateMedicineFromMain(entity.medicine)
    }
}

interface EntityEditOptionsMenu : MenuProvider {
    fun onDestroy()
}

abstract class DatabaseEntityEditFragment<T>(
    private val entityInterface: EntityInterface<T>,
    private val layoutId: Int,
    val name: String,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) :
    Fragment() {

    interface EntityInterface<T> {
        fun getEntity(medicineViewModel: MedicineViewModel, id: Int): T?
        fun updateEntity(medicineViewModel: MedicineViewModel, entity: T)
    }

    private var entity: T? = null
    private var fragmentView: View? = null
    protected lateinit var medicineViewModel: MedicineViewModel
    private var fragmentReady = false
    protected lateinit var optionsMenu: EntityEditOptionsMenu

    protected val idlingResource = SimpleIdlingResource(name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        idlingResource.setBusy()
        fragmentView = inflater.inflate(layoutId, container, false)

        // Do not enter fragment just yet, first fetch entity from database and setup UI
        postponeEnterTransition()

        val navController = findNavController()

        lifecycleScope.launch(ioDispatcher) {
            entity = entityInterface.getEntity(medicineViewModel, getEntityId())

            if (entity != null) {
                setupMenu(navController, entity!!)

                lifecycleScope.launch(mainDispatcher) {
                    if (::optionsMenu.isInitialized) {
                        requireActivity().addMenuProvider(optionsMenu, viewLifecycleOwner)
                    }

                    // Signal that entity was loaded
                    if (activity != null && onEntityLoaded(entity!!, fragmentView!!)) {
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
        idlingResource.setIdle()
        // Now enter fragment
        startPostponedEnterTransition()
    }

    protected open fun setupMenu(navController: NavController, entity: T) {
        optionsMenu = OptionsMenu(
            this,
            ViewModelProvider(this)[MedicineViewModel::class.java],
            navController,
            true
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        idlingResource.destroy()
        if (::optionsMenu.isInitialized) {
            optionsMenu.onDestroy()
        }
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