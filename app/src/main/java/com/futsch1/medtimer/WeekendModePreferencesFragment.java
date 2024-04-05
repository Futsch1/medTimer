package com.futsch1.medtimer;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.futsch1.medtimer.helpers.TimeHelper;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.List;
import java.util.stream.Collectors;

public class WeekendModePreferencesFragment extends PreferenceFragmentCompat {

    public static final String WEEKEND_TIME = "weekend_time";

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.weekend_mode_preferences, rootKey);

        setupTimePicker();
        setupDays();
    }

    private void setupTimePicker() {
        Preference preference = getPreferenceScreen().findPreference(WEEKEND_TIME);
        if (preference != null) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            preference.setSummary(TimeHelper.minutesToTime(defaultSharedPreferences.getInt(WEEKEND_TIME, 540)));
            preference.setOnPreferenceClickListener(preference1 -> {
                int weekendTime = defaultSharedPreferences.getInt(WEEKEND_TIME, 540);
                new TimeHelper.TimePickerWrapper(requireContext()).show(weekendTime / 60, weekendTime % 60, minutes -> {
                    defaultSharedPreferences.edit().putInt(WEEKEND_TIME, minutes).apply();
                    preference1.setSummary(TimeHelper.minutesToTime(minutes));
                });
                return true;
            });
        }
    }

    private void setupDays() {
        Preference preference = getPreferenceScreen().findPreference("weekend_days");
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<MultiSelectListPreference>) preference1 -> {
                @SuppressWarnings("java:S6204") // Using SDK 33
                List<CharSequence> values = preference1.getValues().stream().map(s -> preference1.getEntries()[Integer.parseInt(s) - 1]).collect(Collectors.toList());
                return String.join(", ", values);
            });
        }
    }
}
