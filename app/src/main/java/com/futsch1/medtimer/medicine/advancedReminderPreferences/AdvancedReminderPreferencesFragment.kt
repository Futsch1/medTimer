package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.ModelDataPreferencesFragment
import com.futsch1.medtimer.helpers.ModelDataStore
import com.futsch1.medtimer.helpers.ModelDataViewModel
import com.futsch1.medtimer.model.Reminder
import javax.inject.Inject

abstract class AdvancedReminderPreferencesFragment(
    preferencesResId: Int,
    links: Map<String, (Int) -> NavDirections>,
    customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    simpleSummaryKeys: List<String>
) : ModelDataPreferencesFragment<Reminder>(preferencesResId, links, customOnClick, simpleSummaryKeys) {
    @Inject
    lateinit var reminderDataStoreFactory: ReminderDataStore.Factory

    @Inject
    lateinit var reminderRepository: ReminderRepository

    override suspend fun getDataStore(
        requireArguments: Bundle
    ): ModelDataStore<Reminder> {
        val modelDataId = requireArguments.getInt("reminderId")
        val modelData = reminderRepository.get(modelDataId)!!

        return reminderDataStoreFactory.create(modelData)
    }

    private val reminderViewModel: ReminderViewModel by viewModels()

    override fun getEntityViewModel(): ModelDataViewModel<Reminder> = reminderViewModel

    override fun customSetup(modelData: Reminder) {
        // Intentionally empty
    }
}