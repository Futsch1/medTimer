package com.futsch1.medtimer.preferences

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ReminderNotificationChannelManager.Importance
import com.futsch1.medtimer.helpers.safeStartActivity

class NotificationSettingsFragment : PreferencesFragment() {
    private var rootKey: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        this.rootKey = rootKey
        setPreferencesFromResource(R.xml.notification_settings, rootKey)

        setupNotificationSettings()
        setupExactReminders()
        setupBatteryOptimization()

        setupPreferencesLink(
            this,
            "repeat_reminders_preferences",
            R.id.action_notificationSettingsFragment_to_repeatRemindersPreferencesFragment
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            preferenceScreen.findPreference<Preference?>("sticky_on_lockscreen")?.isVisible = false
        }
    }


    private fun setupNotificationSettings() {
        var preference =
            preferenceScreen.findPreference<Preference?>("notification_settings_high")
        if (preference != null) {
            setupNotificationSettingsPreference(preference, Importance.HIGH)
        }
        preference =
            preferenceScreen.findPreference("notification_settings_default")
        if (preference != null) {
            setupNotificationSettingsPreference(preference, Importance.DEFAULT)
        }
        preference =
            preferenceScreen.findPreference(PreferencesNames.OVERRIDE_DND)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value: Any? ->
                if (true == value) {
                    showDndPermissions()
                }
                true
            }
    }


    private fun setupNotificationSettingsPreference(
        preference: Preference,
        importance: Importance
    ) {
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, importance.value.toString())
            startActivity(intent)
            true
        }
    }


    private fun setupExactReminders() {
        val preference =
            preferenceScreen.findPreference<Preference?>(PreferencesNames.EXACT_REMINDERS)
        if (preference != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                preference.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue: Any? ->
                        if (true == newValue) {
                            showExactReminderDialog()
                        }
                        true
                    }
            } else {
                preference.isVisible = false
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun setupBatteryOptimization() {
        val preference = preferenceScreen.findPreference<Preference?>("ignore_battery_optimization")
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager

        if (powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
            preference?.isVisible = false
        } else {
            preference?.isVisible = true
            preference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:${requireContext().packageName}".toUri()
                safeStartActivity(context, intent)
                true
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun showExactReminderDialog() {
        val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(R.string.enable_alarm_dialog)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                safeStartActivity(context, intent)
            }
            builder.setNegativeButton(R.string.cancel) { _, _ ->
                try {
                    resetBooleanPreferenceAndReload(PreferencesNames.EXACT_REMINDERS)
                } catch (_: IllegalStateException) {
                    // Intentionally empty (monkey test can cause this to fail)
                }
            }
            val d = builder.create()
            d.show()
        }
    }

    private fun resetBooleanPreferenceAndReload(preferenceName: String) {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putBoolean(preferenceName, false)
        }
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }


    private fun cancelOverrideDnd() {
        try {
            resetBooleanPreferenceAndReload(PreferencesNames.OVERRIDE_DND)
        } catch (_: IllegalStateException) {
            // Intentionally empty (monkey test can cause this to fail)
        }
    }


    private fun showDndPermissions() {
        if (!requireContext().getSystemService(NotificationManager::class.java)
                .isNotificationPolicyAccessGranted
        ) {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(R.string.enable_dnd_dialog)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }

            builder.setNegativeButton(R.string.cancel) { _, _ -> cancelOverrideDnd() }
            builder.show()
        }
    }

}
