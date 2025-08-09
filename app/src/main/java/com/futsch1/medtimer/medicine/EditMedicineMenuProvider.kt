package com.futsch1.medtimer.medicine

import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.navigation.NavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper

class EditMedicineMenuProvider(
    private val medicineId: Int,
    private val thread: HandlerThread,
    private val medicineViewModel: MedicineViewModel,
    private val navController: NavController
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_medicine, menu)
        menu.setGroupDividerEnabled(true)

        MedicinesMenu.setupActivateMenu(menu, R.id.activate_all) {
            setRemindersActive(true)
        }
        MedicinesMenu.setupActivateMenu(menu, R.id.deactivate_all) {
            setRemindersActive(false)
        }
        setupDeleteMenu(menu)
    }

    private fun setupDeleteMenu(menu: Menu) {
        menu.findItem(R.id.delete_medicine).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(navController.context)
            deleteHelper.deleteItem(R.string.are_you_sure_delete_medicine, {
                medicineViewModel.medicineRepository.deleteMedicine(medicineId)
                navController.navigateUp()
            }, {
                // do nothing
            })
            true
        }
    }

    private fun setRemindersActive(active: Boolean) {
        @Suppress("kotlin:S6619", "SENSELESS_COMPARISON") // This actually happens due to a race condition
        if (thread.looper.queue == null) {
            return
        }
        val handler = Handler(thread.looper)
        handler.post {
            val reminders: List<Reminder> =
                medicineViewModel.medicineRepository.getReminders(medicineId)
            for (reminder in reminders) {
                reminder.active = active
                medicineViewModel.medicineRepository.updateReminder(reminder)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

}