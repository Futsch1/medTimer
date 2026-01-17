package com.futsch1.medtimer.medicine.settings

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.OptionsMenu.enableOptionalIcons
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.medicine.LinkedReminderHandling

class AdvancedReminderSettingsMenuProvider(
    private val fragment: Fragment
) : MenuProvider {

    lateinit var reminder: Reminder
    lateinit var medicineRepository: MedicineRepository

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.advanced_reminder_settings, menu)
        menu.setGroupDividerEnabled(true)
        enableOptionalIcons(menu)

        menu.findItem(R.id.delete_reminder).setOnMenuItemClickListener { _: MenuItem? ->
            if (this::reminder.isInitialized) {
                LinkedReminderHandling(reminder, medicineRepository, fragment.lifecycleScope).deleteReminder(
                    fragment.requireContext(),
                    { NavHostFragment.findNavController(fragment).navigateUp() }, { }
                )
            }
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

}