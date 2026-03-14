package com.futsch1.medtimer.preferences

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.TimeHelper.TimePickerWrapper
import com.futsch1.medtimer.helpers.TimeHelper.minutesToTimeString
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.WEEKEND_DAYS
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.WEEKEND_MODE
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.WEEKEND_TIME
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestScheduleNextNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.stream.Collectors
import javax.inject.Inject

@AndroidEntryPoint
class WeekendModePreferencesFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    private var currentSettings: UserPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.weekend_mode_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWeekendMode()
        setupTimePicker()
        setupDays()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferencesDataSource.preferences.collect { settings ->
                    currentSettings = settings
                    updateTimePicker(settings)
                }
            }
        }
    }

    private fun setupWeekendMode() {
        val preference = preferenceScreen.findPreference<Preference?>(WEEKEND_MODE)
        preference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            requestReschedule()
            true
        }
    }

    private fun updateTimePicker(settings: UserPreferences) {
        val preference = preferenceScreen.findPreference<Preference?>(WEEKEND_TIME)
        preference?.setSummary(minutesToTimeString(requireContext(), settings.weekendTime.hour * 60 + settings.weekendTime.minute.toLong()))
    }

    private fun setupTimePicker() {
        val preference = preferenceScreen.findPreference<Preference?>(WEEKEND_TIME)
        if (preference != null) {
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference1: Preference? ->
                val weekendTime = currentSettings?.weekendTime ?: LocalTime.of(9, 0)
                TimePickerWrapper(requireActivity()).show(weekendTime.hour, weekendTime.minute) { minutes: Int ->
                    preferencesDataSource.setWeekendTime(LocalTime.of(minutes / 60, minutes % 60))
                    preference1!!.setSummary(minutesToTimeString(requireContext(), minutes.toLong()))
                    requestReschedule()
                }
                true
            }
        }
    }

    private fun setupDays() {
        val preference = preferenceScreen.findPreference<Preference?>(WEEKEND_DAYS)
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
            requestScheduleNextNotification(ReminderContext(context))
        }
    }
}
