package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.futsch1.medtimer.OptionsMenu.Companion.enableOptionalIcons
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.medicine.LinkedReminderHandling
import com.futsch1.medtimer.model.Reminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class AdvancedReminderSettingsMenuProvider @AssistedInject constructor(
    @Assisted private val fragment: Fragment,
    private val linkedReminderHandlingFactory: LinkedReminderHandling.Factory,
    private val reminderRepository: ReminderRepository,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MenuProvider {

    @AssistedFactory
    interface Factory {
        fun create(fragment: Fragment): AdvancedReminderSettingsMenuProvider
    }

    lateinit var reminder: Reminder

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.advanced_reminder_settings, menu)
        menu.setGroupDividerEnabled(true)
        enableOptionalIcons(menu)

        menu.findItem(R.id.duplicate).setOnMenuItemClickListener { _: MenuItem? ->
            if (this::reminder.isInitialized) {
                fragment.lifecycleScope.launch(ioDispatcher) {
                    reminderRepository.create(reminder.copy(id = 0))
                }
                NavHostFragment.findNavController(fragment).navigateUp()
            }
            true
        }

        menu.findItem(R.id.delete_reminder).setOnMenuItemClickListener { _: MenuItem? ->
            if (this::reminder.isInitialized) {
                linkedReminderHandlingFactory.create(reminder, fragment.lifecycleScope).deleteReminder(
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
