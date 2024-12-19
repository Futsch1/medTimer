package com.futsch1.medtimer.medicine

import android.os.HandlerThread
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.navigation.Navigation.findNavController
import com.futsch1.medtimer.MedicineViewModel
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder

class AdvancedReminderSettingsMenuProvider(
    private val reminder: Reminder,
    private val thread: HandlerThread,
    private val medicineViewModel: MedicineViewModel,
    private val advancedReminderView: View
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.advanced_reminder_settings, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)

        menu.findItem(R.id.delete_reminder).setOnMenuItemClickListener { _: MenuItem? ->
            LinkedReminderHandling(reminder, medicineViewModel).deleteReminder(
                advancedReminderView,
                thread
            ) { findNavController(advancedReminderView).navigateUp() }
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

}