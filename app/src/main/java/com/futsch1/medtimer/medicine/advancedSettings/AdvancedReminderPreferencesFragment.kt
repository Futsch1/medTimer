package com.futsch1.medtimer.medicine.advancedSettings

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.database.Reminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class AdvancedReminderPreferencesFragment(
    val preferencesResId: Int,
    val links: Map<String, (Int) -> NavDirections>,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PreferenceFragmentCompat() {
    var reminderId: Int = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        reminderId = AdvancedReminderPreferencesRootFragmentArgs.fromBundle(requireArguments()).getReminderId()
        val reminderViewModel = ViewModelProvider(this)[ReminderViewModel::class.java]

        this.lifecycleScope.launch(ioDispatcher) {
            preferenceManager.preferenceDataStore = ReminderDataStore(reminderId, reminderViewModel.medicineRepository)
            this.launch(mainDispatcher) {
                setPreferencesFromResource(preferencesResId, rootKey)
                setupLinks()
            }
        }

        observeUserData(reminderViewModel, reminderId)
    }

    private fun observeUserData(reminderViewModel: ReminderViewModel, reminderId: Int) {
        lifecycleScope.launch {
            reminderViewModel.getReminderFlow(reminderId).collect { reminder ->
                onReminderUpdated(reminder)
            }
        }
    }

    private fun setupLinks() {
        val navController = findNavController(this.requireView())

        for (link in links) {
            val preference = findPreference<Preference?>(link.key)
            preference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                try {
                    navController.navigate(link.value(reminderId))
                } catch (_: IllegalArgumentException) {
                    // Intentionally empty (monkey test can cause this to fail)
                }
                true
            }
        }
    }

    open fun onReminderUpdated(reminder: Reminder) {
        // Intentionally empty
    }
}