package com.futsch1.medtimer.medicine.advancedSettings

import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder


class AdvancedReminderPreferencesIntervalType(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_interval_type,
    mapOf(
    ),
    mapOf(
        "interval_start_time" to { activity, preference -> showDateTimeEdit(activity, preference) },
        "interval_daily_start_time" to { activity, preference -> showTimeEdit(activity, preference) },
        "interval_daily_end_time" to { activity, preference -> showTimeEdit(activity, preference) }
    ),
    listOf("interval_start_time", "interval_daily_start_time", "interval_daily_end_time")
) {

    override fun onReminderUpdated(reminder: Reminder) {
        super.onReminderUpdated(reminder)
        findPreference<Preference>("interval_start_time")?.isEnabled = !reminder.dailyInterval
    }

}