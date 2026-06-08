package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import com.futsch1.medtimer.feature.ui.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdvancedReminderPreferencesInstructionsFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_instructions,
    mapOf(),
    mapOf(),
    listOf("instructions")
)