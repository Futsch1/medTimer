package com.futsch1.medtimer.medicine.advancedReminderPreferences

import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedReminderPreferencesInstructionsFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_instructions,
    mapOf(),
    mapOf(),
    listOf("instructions")
) {
    @Inject
    override lateinit var medicineRepository: MedicineRepository
}