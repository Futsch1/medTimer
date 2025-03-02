package com.futsch1.medtimer.preferences;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.RequiresApi;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.futsch1.medtimer.MainActivity;
import com.futsch1.medtimer.R;
import com.futsch1.medtimer.ReminderNotificationChannelManager;


public class PreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        setupTheme();
        setupExactReminders();
        setupNotificationSettings();
        setupPreferencesLink("repeat_reminders_preferences", R.id.action_preferencesFragment_to_repeatRemindersPreferencesFragment);
        setupPreferencesLink("weekend_mode", R.id.action_preferencesFragment_to_weekendModePreferencesFragment);
        setupBlockScreenCapture();
    }

    private void setupTheme() {
        Preference preference = getPreferenceScreen().findPreference("theme");
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference1, newValue) -> {
                try {
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    requireActivity().finish();
                    startActivity(intent);
                    return true;
                } catch (IllegalStateException e) {
                    return false;
                }
            });
        }
    }

    private void setupExactReminders() {
        Preference preference = getPreferenceScreen().findPreference(PreferencesNames.EXACT_REMINDERS);
        if (preference != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                preference.setOnPreferenceChangeListener((preference13, newValue) -> {
                    if (Boolean.TRUE.equals(newValue)) {
                        showExactReminderDialog();
                    }
                    return true;
                });
            } else {
                preference.setVisible(false);
            }
        }
    }

    private void setupNotificationSettings() {
        Preference preference = getPreferenceScreen().findPreference("notification_settings_high");
        if (preference != null) {
            setupNotificationSettingsPreference(preference, ReminderNotificationChannelManager.Importance.HIGH);
        }
        preference = getPreferenceScreen().findPreference("notification_settings_default");
        if (preference != null) {
            setupNotificationSettingsPreference(preference, ReminderNotificationChannelManager.Importance.DEFAULT);
        }
        preference = getPreferenceScreen().findPreference(PreferencesNames.OVERRIDE_DND);
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference1, value) -> {
                if (Boolean.TRUE.equals(value)) {
                    showDndPermissions();
                }
                return true;
            });
        }
    }

    private void setupPreferencesLink(String preferenceKey, @IdRes int actionId) {
        Preference preference = getPreferenceScreen().findPreference(preferenceKey);
        if (preference != null) {
            preference.setOnPreferenceClickListener(preference1 ->
                    {
                        NavController navController = Navigation.findNavController(requireView());
                        try {
                            navController.navigate(actionId);
                        } catch (IllegalArgumentException e) {
                            // Intentionally empty
                        }
                        return true;
                    }
            );
        }
    }

    private void setupBlockScreenCapture() {
        SwitchPreference preference = getPreferenceScreen().findPreference("window_flag_secure");
        if (preference != null) {
            preference.setOnPreferenceChangeListener((preference12, newValue) -> {
                requireActivity().getWindow().setFlags(Boolean.TRUE.equals(newValue) ? WindowManager.LayoutParams.FLAG_SECURE : 0, WindowManager.LayoutParams.FLAG_SECURE);
                return true;
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void showExactReminderDialog() {
        AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
        if (!alarmManager.canScheduleExactAlarms()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.enable_alarm_dialog).
                    setPositiveButton(R.string.ok, (dialog, id) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        safeStartActivity(intent);
                    }).
                    setNegativeButton(R.string.cancel, (dialog, id) -> {
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean(PreferencesNames.EXACT_REMINDERS, false).apply();
                        setPreferenceScreen(null);
                        addPreferencesFromResource(R.xml.root_preferences);
                    });
            AlertDialog d = builder.create();
            d.show();
        }
    }

    private void setupNotificationSettingsPreference(Preference preference, ReminderNotificationChannelManager.Importance importance) {
        preference.setOnPreferenceClickListener(preference1 ->
                {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, Integer.toString(importance.getValue()));
                    startActivity(intent);
                    return true;
                }
        );
    }

    private void showDndPermissions() {
        if (!requireContext().getSystemService(NotificationManager.class).isNotificationPolicyAccessGranted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.enable_dnd_dialog).
                    setPositiveButton(R.string.ok, (dialog, id) -> {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        safeStartActivity(intent);
                    }).
                    setNegativeButton(R.string.cancel, (dialog, id) -> {
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean(PreferencesNames.OVERRIDE_DND, false).apply();
                        setPreferenceScreen(null);
                        addPreferencesFromResource(R.xml.root_preferences);
                    });
            AlertDialog d = builder.create();
            d.show();
        }
    }

    private void safeStartActivity(Intent intent) {
        try {
            startActivity(intent);
        } catch (IllegalStateException e) {
            // Intentionally empty
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeExactReminders();
        resumeOverrideDnd();
    }

    private void resumeExactReminders() {
        SwitchPreferenceCompat preference = getPreferenceScreen().findPreference(PreferencesNames.EXACT_REMINDERS);
        if (preference != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = requireContext().getSystemService(AlarmManager.class);
            if (!alarmManager.canScheduleExactAlarms()) {
                preference.setChecked(false);
            }
        }
    }

    private void resumeOverrideDnd() {
        SwitchPreferenceCompat preference;
        preference = getPreferenceScreen().findPreference(PreferencesNames.OVERRIDE_DND);
        if (preference != null && !requireContext().getSystemService(NotificationManager.class).isNotificationPolicyAccessGranted()) {
            preference.setChecked(false);
        }
    }

}