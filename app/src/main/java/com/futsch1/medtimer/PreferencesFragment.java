package com.futsch1.medtimer;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.takisoft.preferencex.RingtonePreference;

public class PreferencesFragment extends PreferenceFragmentCompat {
    public static final String EXACT_REMINDERS = "exact_reminders";

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        setupShowNotifications();
        setupTheme();
        setupExactReminders();
        setupNotificationTone();
        setupWeekendMode();
    }

    private void setupShowNotifications() {
        if (ContextCompat.checkSelfPermission(requireContext(), POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Preference preference = getPreferenceScreen().findPreference("show_notification");
            if (preference != null) {
                preference.setEnabled(false);
                preference.setSummary(R.string.permission_not_granted);
            }
        }
    }

    private void setupTheme() {
        Preference preference = getPreferenceScreen().findPreference("theme");
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference1, newValue) -> {
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                requireActivity().finish();
                startActivity(intent);
                return true;
            });
        }
    }

    private void setupExactReminders() {
        Preference preference = getPreferenceScreen().findPreference(EXACT_REMINDERS);
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference13, newValue) -> {
                if (Boolean.TRUE.equals(newValue)) {
                    AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
                    if (!alarmManager.canScheduleExactAlarms()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.enable_alarm_dialog).
                                setPositiveButton(R.string.ok, (dialog, id) -> {
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                    requireContext().startActivity(intent);
                                }).
                                setNegativeButton(R.string.cancel, (dialog, id) -> {
                                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean(EXACT_REMINDERS, false).apply();
                                    setPreferenceScreen(null);
                                    addPreferencesFromResource(R.xml.root_preferences);
                                });
                        AlertDialog d = builder.create();
                        d.show();
                    }
                }
                return true;
            });
        }
    }

    private void setupNotificationTone() {
        RingtonePreference preference = getPreferenceScreen().findPreference("notification_ringtone");
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference1, newValue) -> {
                NotificationChannelManager.updateNotificationChannel(requireContext(), (Uri) newValue);
                return true;
            });
        }
    }

    private void setupWeekendMode() {
        Preference preference = getPreferenceScreen().findPreference("weekend_mode");
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 ->
                    {
                        Navigation.findNavController(requireView()).navigate(R.id.action_preferencesFragment_to_weekendModePreferencesFragment);
                        return true;
                    }
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SwitchPreferenceCompat preference = getPreferenceScreen().findPreference(EXACT_REMINDERS);
        if (preference != null) {
            AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
            if (!alarmManager.canScheduleExactAlarms()) {
                preference.setChecked(false);
            }
        }
    }

}