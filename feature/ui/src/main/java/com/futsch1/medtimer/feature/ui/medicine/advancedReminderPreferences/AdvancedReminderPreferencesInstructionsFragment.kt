package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import androidx.preference.EditTextPreference
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.feature.ui.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdvancedReminderPreferencesInstructionsFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_instructions,
    mapOf(),
    mapOf(),
    listOf("instructions")
) {
    override fun onModelDataUpdated(modelData: Reminder) {
        super.onModelDataUpdated(modelData)
        findPreference<EditTextPreference>("instructions")?.let { preference ->
            if (preference.text != modelData.instructions) {
                preference.text = modelData.instructions
            }
        }
    }
}