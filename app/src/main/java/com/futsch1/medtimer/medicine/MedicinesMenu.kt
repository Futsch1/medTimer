package com.futsch1.medtimer.medicine

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.setAllRemindersActive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicinesMenu(
    private val medicineViewModel: MedicineViewModel,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.medicines, menu)
        menu.setGroupDividerEnabled(true)

        setupActivateMenu(menu, R.id.activate_all) {
            setRemindersActive(true)
        }
        setupActivateMenu(menu, R.id.deactivate_all) {
            setRemindersActive(false)
        }
    }

    private fun setRemindersActive(active: Boolean) {
        medicineViewModel.viewModelScope.launch(dispatcher) {
            for (medicine in medicineViewModel.medicineRepository.medicines) {
                setAllRemindersActive(medicine, medicineViewModel.medicineRepository, active)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    companion object {
        fun setupActivateMenu(
            menu: Menu, menuId: Int, activateCallback: () -> Unit
        ) {
            menu.findItem(menuId).setOnMenuItemClickListener { _: MenuItem? ->
                activateCallback()
                true
            }
        }
    }
}
