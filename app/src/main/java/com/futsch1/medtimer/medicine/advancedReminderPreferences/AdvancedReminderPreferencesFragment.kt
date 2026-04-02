package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.preference.Preference
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.EntityPreferencesFragment
import com.futsch1.medtimer.helpers.EntityViewModel
import javax.inject.Inject

abstract class AdvancedReminderPreferencesFragment(
    preferencesResId: Int,
    links: Map<String, (Int) -> NavDirections>,
    customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>,
    simpleSummaryKeys: List<String>
) : EntityPreferencesFragment<ReminderEntity>(preferencesResId, links, customOnClick, simpleSummaryKeys) {
    @Inject
    lateinit var reminderDataStoreFactory: ReminderDataStore.Factory

    @Inject
    lateinit var medicineRepository: MedicineRepository

    override suspend fun getEntityDataStore(
        requireArguments: Bundle
    ): EntityDataStore<ReminderEntity> {
        val entityId = requireArguments.getInt("reminderId")
        val entity = medicineRepository.getReminder(entityId)!!

        return reminderDataStoreFactory.create(entity)
    }

    private val reminderViewModel: ReminderViewModel by viewModels()

    override fun getEntityViewModel(): EntityViewModel<ReminderEntity> = reminderViewModel

    override fun customSetup(entity: ReminderEntity) {
        // Intentionally empty
    }
}