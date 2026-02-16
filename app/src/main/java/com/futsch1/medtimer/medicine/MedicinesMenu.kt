package com.futsch1.medtimer.medicine

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.helpers.setAllRemindersActive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicinesMenu(
    private val medicineViewModel: MedicineViewModel,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MenuProvider {

    lateinit var medicines: List<FullMedicine>

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.medicines, menu)
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

    private fun sortBy(sortFunction: (List<FullMedicine>) -> List<FullMedicine>) {
        medicineViewModel.viewModelScope.launch(dispatcher) {
            val medicines = sortFunction(medicines)
            medicines.stream().forEach { it.medicine.sortOrder = 1.0 + medicines.indexOf(it) }
            medicineViewModel.medicineRepository.updateMedicines(medicines.stream().map { it.medicine }.toList())
        }
    }

    private fun setRemindersActive(active: Boolean) {
        medicineViewModel.viewModelScope.launch(dispatcher) {
            if (this@MedicinesMenu::medicines.isInitialized) {
                for (fullMedicine in medicines) {
                    setAllRemindersActive(fullMedicine, medicineViewModel.medicineRepository, active)
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

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
