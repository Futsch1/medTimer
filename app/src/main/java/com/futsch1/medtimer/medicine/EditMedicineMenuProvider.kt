package com.futsch1.medtimer.medicine

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.navigation.Navigation.findNavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.DeleteHelper

class EditMedicineMenuProvider(
    private val medicineId: Int,
    private val thread: HandlerThread,
    private val medicineViewModel: MedicineViewModel,
    private val fragmentEditMedicine: View
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_medicine, menu)
        menu.setGroupDividerEnabled(true)

        menu.findItem(R.id.activate_all).setOnMenuItemClickListener { _: MenuItem? ->
            setRemindersActive(true)
            true
        }
        menu.findItem(R.id.deactivate_all).setOnMenuItemClickListener { _: MenuItem? ->
            setRemindersActive(false)
            true
        }
        setupDeleteMenu(menu)
    }

    private fun setupDeleteMenu(menu: Menu) {
        menu.findItem(R.id.delete_medicine).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(fragmentEditMedicine.context)
            deleteHelper.deleteItem(R.string.are_you_sure_delete_medicine, {
                val threadHandler = Handler(thread.getLooper())
                threadHandler.post {
                    medicineViewModel.deleteMedicine(medicineViewModel.getMedicine(medicineId))
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post {
                        val navController = findNavController(fragmentEditMedicine)
                        navController.navigateUp()
                    }
                }
            }, {
                // do nothing
            })
            true
        }
    }


    private fun setRemindersActive(active: Boolean) {
        val handler = Handler(thread.getLooper())
        handler.post {
            val reminders: List<Reminder> =
                medicineViewModel.getReminders(medicineId)
            for (reminder in reminders) {
                reminder.active = active
                medicineViewModel.updateReminder(reminder)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

}