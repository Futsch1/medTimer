package com.futsch1.medtimer.medicine

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MedicinesMenu @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:Dispatcher(MedTimerDispatchers.IO) private val dispatcher: CoroutineDispatcher
) : MenuProvider {

    lateinit var medicines: List<FullMedicineEntity>

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.medicines_settings, menu)
        menu.setGroupDividerEnabled(true)

        setupMenu(menu, R.id.activate_all) {
            setRemindersActive(true)
        }
        setupMenu(menu, R.id.deactivate_all) {
            setRemindersActive(false)
        }
        setupMenu(menu, R.id.sortByName) {
            sortBy { m -> m.sortedBy { it.medicine.name } }
        }
        setupMenu(menu, R.id.sortByCreationDateAsc) {
            sortBy { m -> m.sortedBy { it.medicine.medicineId } }
        }
        setupMenu(menu, R.id.sortByCreationDateDesc) {
            sortBy { m -> m.sortedByDescending { it.medicine.medicineId } }
        }
    }

    private fun sortBy(sortFunction: (List<FullMedicineEntity>) -> List<FullMedicineEntity>) {
        applicationScope.launch(dispatcher) {
            val medicines = sortFunction(medicines)
            medicines.stream().forEach { it.medicine.sortOrder = 1.0 + medicines.indexOf(it) }
            medicineRepository.updateAll(medicines.stream().map { it.medicine }.toList())
        }
    }

    private fun setRemindersActive(active: Boolean) {
        applicationScope.launch(dispatcher) {
            if (this@MedicinesMenu::medicines.isInitialized) {
                for (fullMedicine in medicines) {
                    val localMedicine = medicineRepository.getFull(fullMedicine.medicine.medicineId)
                    com.futsch1.medtimer.helpers.setRemindersActive(localMedicine!!.reminders.map { it.toModel() }, reminderRepository, active)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    companion object {
        fun setupMenu(
            menu: Menu, menuId: Int, activateCallback: () -> Unit
        ) {
            menu.findItem(menuId).setOnMenuItemClickListener { _: MenuItem? ->
                activateCallback()
                true
            }
        }
    }
}
