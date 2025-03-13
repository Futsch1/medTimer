package com.futsch1.medtimer.overview

import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.navigation.Navigation.findNavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.DeleteHelper
import com.futsch1.medtimer.reminders.ReminderProcessor

class EditEventMenuProvider(
    private val reminderEventId: Int,
    private val thread: HandlerThread,
    private val medicineViewModel: MedicineViewModel,
    private val fragmentEditEvent: View
) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_event, menu)
        menu.setGroupDividerEnabled(true)
        setupDeleteMenu(menu)
        setupReRaiseMenu(menu)
    }

    private fun setupDeleteMenu(menu: Menu) {
        menu.findItem(R.id.delete_event).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(fragmentEditEvent.context)
            deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder_event, {
                val threadHandler = Handler(thread.looper)
                threadHandler.post {
                    val reminderEvent =
                        medicineViewModel.medicineRepository.getReminderEvent(reminderEventId)
                    if (reminderEvent != null) {
                        reminderEvent.status = ReminderEvent.ReminderStatus.DELETED
                        medicineViewModel.medicineRepository.updateReminderEvent(reminderEvent)
                    }
                }
                val navController = findNavController(fragmentEditEvent)
                navController.navigateUp()
            }, {
                // do nothing
            })
            true
        }
    }

    private fun setupReRaiseMenu(menu: Menu) {
        menu.findItem(R.id.re_raise).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(fragmentEditEvent.context)
            deleteHelper.deleteItem(R.string.delete_re_raise_event, {
                medicineViewModel.medicineRepository.deleteReminderEvent(reminderEventId)
                ReminderProcessor.requestReschedule(fragmentEditEvent.context)
                val navController = findNavController(fragmentEditEvent)
                navController.navigateUp()
            }, {
                // do nothing
            })
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }
}