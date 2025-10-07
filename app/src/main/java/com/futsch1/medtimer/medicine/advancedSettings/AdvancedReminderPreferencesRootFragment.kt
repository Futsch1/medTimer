package com.futsch1.medtimer.medicine.advancedSettings

import android.text.InputType
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.DatePickerWrapper


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
        }
    ),
    mapOf(
        "cycle_start_date" to { activity, preference -> showDateEdit(activity, preference) }
    ),
    listOf("instructions", "cycle_start_date", "cycle_consecutive_days", "cycle_pause_days")
) {
    override fun onReminderUpdated(reminder: Reminder) {
        super.onReminderUpdated(reminder)

        findPreference<Preference>("interval_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.INTERVAL_BASED
        findPreference<Preference>("cycle_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.TIME_BASED
        findPreference<Preference>("interval")?.summary = Interval(reminder.timeInMinutes).toTranslatedString(requireContext())
    }

    override fun customSetup(reminder: Reminder) {
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