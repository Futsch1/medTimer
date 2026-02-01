package com.futsch1.medtimer.medicine.advancedReminderPreferences

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

    override fun onEntityUpdated(entity: Reminder) {
        super.onEntityUpdated(entity)

        findPreference<Preference>("period_start_date")?.isVisible = entity.periodStart != 0L
        findPreference<Preference>("period_end_date")?.isVisible = entity.periodEnd != 0L
    }

}