package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
    override suspend fun getEntityDataStore(
        requireArguments: Bundle
    ): EntityDataStore<Reminder> {
        val entityId = requireArguments.getInt("reminderId")
        val entity = medicineRepository.getReminder(entityId)!!

        return ReminderDataStore(
            entity,
            requireContext(),
            medicineRepository,
            lifecycleScope
        )
    }

    override fun getEntityViewModel(): EntityViewModel<Reminder> {
        return ViewModelProvider(this)[ReminderViewModel::class.java]
    }

    override fun customSetup(entity: Reminder) {
        // Intentionally empty
    }
}