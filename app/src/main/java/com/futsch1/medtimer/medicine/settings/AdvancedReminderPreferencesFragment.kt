package com.futsch1.medtimer.medicine.settings

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.database.Reminder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class AdvancedReminderPreferencesFragment(
    val preferencesResId: Int,
    val links: Map<String, (Int) -> NavDirections>,
    val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    val simpleSummaryKeys: List<String>,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PreferenceFragmentCompat() {
    var reminderId: Int = 0
    lateinit var reminderDataStore: ReminderDataStore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        reminderId = AdvancedReminderPreferencesRootFragmentArgs.fromBundle(requireArguments()).getReminderId()
        val reminderViewModel = ViewModelProvider(this)[ReminderViewModel::class.java]

        postponeEnterTransition()

        this.lifecycleScope.launch(ioDispatcher) {
            try {
                reminderDataStore = ReminderDataStore(reminderId, requireContext(), reminderViewModel.medicineRepository)
                preferenceManager.preferenceDataStore = reminderDataStore
                this.launch(mainDispatcher) {
                    setPreferencesFromResource(preferencesResId, rootKey)
                    observeUserData(reminderViewModel, reminderId)
                    setupLinks()
                    setupOnClick()
                    customSetup(reminderDataStore.reminder)

                    startPostponedEnterTransition()
                }
            } catch (_: NullPointerException) {
                // It may happen that the reminder is deleted already, so ignore this
            }
        }
    }

    open fun customSetup(reminder: Reminder) {
        // Intentionally empty
    }

    private fun observeUserData(reminderViewModel: ReminderViewModel, reminderId: Int) {
        lifecycleScope.launch {
            reminderViewModel.getReminderFlow(reminderId).collect { reminder ->
                if (reminder == null) {
                    return@collect
                }
                reminderDataStore.reminder = reminder
                onReminderUpdated(reminder)
            }
        }
    }

    private fun setupLinks() {
        try {
            val navController = findNavController()

            for (link in links) {
                val preference = findPreference<Preference?>(link.key)
                preference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                    try {
                        navController.navigate(link.value(reminderId))
                    } catch (_: IllegalArgumentException) {
                        // Intentionally empty (monkey test can cause this to fail)
                    }
                    true
                }
            }
        } catch (_: IllegalStateException) {
            // Ignore this
        }
    }

    private fun setupOnClick() {
        for (onClick in customOnClick) {
            val preference = findPreference<Preference?>(onClick.key)
            preference!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                onClick.value(requireActivity(), preference)
                true
            }
        }
    }

    open fun onReminderUpdated(reminder: Reminder) {
        for (simpleSummaryKey in simpleSummaryKeys) {
            val preference = findPreference<Preference?>(simpleSummaryKey)
            preference!!.summary = preferenceManager.preferenceDataStore?.getString(simpleSummaryKey, "?")
        }
    }
}