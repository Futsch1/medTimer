package com.futsch1.medtimer.preferences

import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestScheduleNextNotification
import java.util.stream.Collectors

class WeekendModePreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.weekend_mode_preferences, rootKey)

        setupWeekendMode()
        setupTimePicker()
        setupDays()
    }

    private fun setupWeekendMode() {
        val preference = preferenceScreen.findPreference<Preference?>(PreferencesNames.WEEKEND_MODE)
        preference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            requestReschedule()
            true
        }
    }

    private fun setupTimePicker() {
        val preference = preferenceScreen.findPreference<Preference?>(PreferencesNames.WEEKEND_TIME)
        if (preference != null) {
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            preference.setSummary(minutesToTimeString(requireContext(), defaultSharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, 540).toLong()))
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference1: Preference? ->
                val weekendTime = defaultSharedPreferences.getInt(PreferencesNames.WEEKEND_TIME, 540)
                TimePickerWrapper(requireActivity()).show(weekendTime / 60, weekendTime % 60) { minutes: Int ->
                    defaultSharedPreferences.edit { putInt(PreferencesNames.WEEKEND_TIME, minutes) }
                    preference1!!.setSummary(minutesToTimeString(requireContext(), minutes.toLong()))
                    requestReschedule()
                }
                true
            }
        }
    }

    private fun setupDays() {
        val preference = preferenceScreen.findPreference<Preference?>(PreferencesNames.WEEKEND_DAYS)
        if (preference != null) {
            preference.setSummaryProvider(SummaryProvider { preference1: MultiSelectListPreference? ->
                preference1!!.values.stream().map { s: String? -> preference1.entries[s!!.toInt() - 1] }
                    .collect(Collectors.toList()).joinToString(", ")

            })
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                requestReschedule()
                true
            }
        }
    }

    private fun requestReschedule() {
        val context = getContext()
        if (context != null) {
            requestScheduleNextNotification(context)
        }
    }
}
