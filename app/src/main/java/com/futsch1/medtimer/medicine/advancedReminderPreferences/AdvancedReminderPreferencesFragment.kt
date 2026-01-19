package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel

abstract class AdvancedReminderPreferencesFragment(
    preferencesResId: Int,
    links: Map<String, (Int) -> NavDirections>,
    customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    simpleSummaryKeys: List<String>
) : EntityPreferencesFragment<Reminder>(preferencesResId, links, customOnClick, simpleSummaryKeys) {
    override fun getEntityDataStore(
        requireArguments: Bundle
    ): EntityDataStore<Reminder> {
        return ReminderDataStore(requireArguments.getInt("reminderId"), requireContext(), getEntityViewModel().medicineRepository)
    }

    override fun getEntityViewModel(): EntityViewModel<Reminder> {
        return ViewModelProvider(this)[ReminderViewModel::class.java]
    }

    override fun customSetup(entity: Reminder) {
        // Intentionally empty
    }
}