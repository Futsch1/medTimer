package com.futsch1.medtimer.medicine.advancedSettings

import com.futsch1.medtimer.R


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

}