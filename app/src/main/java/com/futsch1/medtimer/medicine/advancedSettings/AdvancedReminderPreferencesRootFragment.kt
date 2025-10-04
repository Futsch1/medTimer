package com.futsch1.medtimer.medicine.advancedSettings

import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder

class AdvancedReminderPreferencesRootFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_root, mapOf(
        "instructions" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesInstructionsFragment(
                id
            )
        }
    )
) {
    override fun onReminderUpdated(reminder: Reminder) {
        val instructionPreference = findPreference<Preference>("instructions")
        instructionPreference?.summary = reminder.instructions

        val intervalPreference = findPreference<Preference>("interval_category")
        intervalPreference?.isVisible = reminder.reminderType == Reminder.ReminderType.INTERVAL_BASED

        val cyclePreference = findPreference<Preference>("cycle_category")
        cyclePreference?.isVisible = reminder.reminderType == Reminder.ReminderType.TIME_BASED


    }

}