package com.futsch1.medtimer.medicine

import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder

class MedicinesMenu(
    private val medicineViewModel: MedicineViewModel,
    private val thread: HandlerThread
) : MenuProvider {

    lateinit var medicinesList: List<FullMedicine>

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
        val handler = Handler(thread.getLooper())
        handler.post {
            for (medicine in medicinesList) {
                val reminders: List<Reminder> =
                    medicineViewModel.medicineRepository.getReminders(medicine.medicine.medicineId)
                for (reminder in reminders) {
                    reminder.active = active
                    medicineViewModel.medicineRepository.updateReminder(reminder)
                }
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
