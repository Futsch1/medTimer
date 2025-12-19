package com.futsch1.medtimer.medicine.advancedSettings

import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder


class AdvancedReminderPreferencesStatusFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_status,
    mapOf(
    ),
    mapOf(
        "period_start_date" to { activity, preference -> showDateEdit(activity, preference) },
        "period_end_date" to { activity, preference -> showDateEdit(activity, preference) }
    ),
    listOf("period_start_date", "period_end_date")
) {

    override fun onReminderUpdated(reminder: Reminder) {
        super.onReminderUpdated(reminder)

        findPreference<Preference>("period_start_date")?.isVisible = reminder.periodStart != 0L
        findPreference<Preference>("period_end_date")?.isVisible = reminder.periodEnd != 0L
    }

}