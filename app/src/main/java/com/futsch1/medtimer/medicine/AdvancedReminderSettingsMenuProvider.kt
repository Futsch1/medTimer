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

class AdvancedReminderSettingsMenuProvider(
    private val reminder: Reminder,
    private val thread: HandlerThread,
    private val medicineViewModel: MedicineViewModel,
    private val advancedReminderView: View
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.advanced_reminder_settings, menu)
        menu.setGroupDividerEnabled(true)

        menu.findItem(R.id.delete_reminder).setOnMenuItemClickListener { _: MenuItem? ->
            val deleteHelper = DeleteHelper(advancedReminderView.context)
            deleteHelper.deleteItem(R.string.are_you_sure_delete_reminder, {
                val threadHandler = Handler(thread.getLooper())
                threadHandler.post {
                    medicineViewModel.deleteReminder(reminder)
                    val mainHandler = Handler(Looper.getMainLooper())
                    mainHandler.post {
                        val navController = findNavController(advancedReminderView)
                        navController.navigateUp()
                    }
                }
            }, {})
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

}