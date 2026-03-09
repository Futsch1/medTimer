package com.futsch1.medtimer.medicine.advancedReminderPreferences

import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository

class AdvancedReminderPreferencesInstructionsFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_instructions,
    mapOf(),
    mapOf(),
    listOf("instructions")
) {
    override val medicineRepository: MedicineRepository by lazy { MedicineRepository(requireContext()) }
}