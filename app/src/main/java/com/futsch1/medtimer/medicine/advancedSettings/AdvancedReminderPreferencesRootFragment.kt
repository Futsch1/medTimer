package com.futsch1.medtimer.medicine.advancedSettings

import android.text.InputType
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.DatePickerWrapper
import com.futsch1.medtimer.helpers.isReminderActive


fun showDateEdit(activity: FragmentActivity, preference: Preference) {
    val datePickerWrapper = DatePickerWrapper(activity, R.string.cycle_start_date)
    val currentDateString = preference.preferenceDataStore?.getString(preference.key, null)
    if (currentDateString != null) {
        val currentDate = TimeHelper.dateStringToDate(activity, currentDateString)
        datePickerWrapper.show(currentDate) { daysSinceEpoch: Long ->
            val newDateString = TimeHelper.daysSinceEpochToDateString(activity, daysSinceEpoch)
            preference.preferenceDataStore?.putString(preference.key, newDateString)
        }

    }
}

class AdvancedReminderPreferencesRootFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_root,
    mapOf(
        "instructions" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesInstructionsFragment(
                id
            )
        },
        "reminder_status" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesStatusFragment(
                id
            )
        }
    ),
    mapOf(
        "cycle_start_date" to { activity, preference -> showDateEdit(activity, preference) }
    ),
    listOf("instructions", "cycle_start_date", "cycle_consecutive_days", "cycle_pause_days")
) {
    override fun onReminderUpdated(reminder: Reminder) {
        super.onReminderUpdated(reminder)

        findPreference<Preference>("reminder_status")?.summary =
            requireContext().getString(if (isReminderActive(reminder)) R.string.active else R.string.inactive)
        findPreference<Preference>("interval")?.summary = Interval(reminder.timeInMinutes).toTranslatedString(requireContext())
        findPreference<MultiSelectListPreference>("remind_on_weekdays")?.summary = getWeekdaysSummary(reminder)
        findPreference<MultiSelectListPreference>("remind_on_days")?.summary = getDaysSummary(reminder)
    }

    private fun getDaysSummary(reminder: Reminder): String {
        return if ((reminder.activeDaysOfMonth and 0x7FFF_FFFF) == 0x7FFF_FFFF) {
            requireContext().getString(R.string.every_day_of_month)
        } else {
            val days: MutableList<String> = mutableListOf()
            for (i in 0..30) {
                if ((reminder.activeDaysOfMonth and (1 shl i)) != 0) {
                    days += (i + 1).toString()
                }
            }
            requireContext().getString(R.string.on_day_of_month, days.joinToString(", "))
        }
    }

    private fun getWeekdaysSummary(reminder: Reminder): String {
        return if (reminder.days.none { it }) {
            requireContext().getString(R.string.never)
        } else if (reminder.days.all { it }) {
            requireContext().getString(R.string.every_day)
        } else {
            val days: MutableList<String> = mutableListOf()
            for ((i, day) in requireContext().resources.getStringArray(R.array.days).withIndex()) {
                if (reminder.days[i]) {
                    days += day
                }
            }
            days.joinToString(", ")
        }
    }

    override fun customSetup(reminder: Reminder) {
        findPreference<Preference>("interval_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.INTERVAL_BASED
        findPreference<Preference>("cycle_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.TIME_BASED
        findPreference<Preference>("time_based_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.TIME_BASED

        findPreference<EditTextPreference>("cycle_consecutive_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        findPreference<EditTextPreference>("cycle_pause_days")?.setOnBindEditTextListener { editText -> editText.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED }

        findPreference<Preference>("interval")?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            EditIntervalDialog(requireContext(), reminder.timeInMinutes) { newIntervalMinutes ->
                preferenceManager.preferenceDataStore?.putInt(
                    "interval",
                    newIntervalMinutes
                )
            }
            true
        }
    }

}