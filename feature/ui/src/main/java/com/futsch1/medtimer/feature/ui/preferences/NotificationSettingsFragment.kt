package com.futsch1.medtimer.feature.ui.preferences

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.futsch1.medtimer.core.common.helpers.safeStartActivity
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource.Companion.CANNOT_SKIP_REMINDERS
import com.futsch1.medtimer.core.datastore.PreferencesDataSource.Companion.DISMISS_NOTIFICATION_ACTION
import com.futsch1.medtimer.core.datastore.PreferencesDataSource.Companion.EXACT_REMINDERS
import com.futsch1.medtimer.core.datastore.PreferencesDataSource.Companion.OVERRIDE_DND
import com.futsch1.medtimer.core.datastore.PreferencesDataSource.Companion.STICKY_ON_LOCKSCREEN
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.feature.ui.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSettingsFragment : PreferencesFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var powerManager: PowerManager

    @Inject
    lateinit var alarmManager: AlarmManager

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

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
            preferenceScreen.findPreference<Preference?>(STICKY_ON_LOCKSCREEN)?.isVisible = false
        }
    }


    private fun setupNotificationSettings() {
        var preference =
            preferenceScreen.findPreference<Preference?>("notification_settings_high")
        if (preference != null) {
            setupNotificationSettingsPreference(preference, Medicine.NotificationImportance.HIGH)
        }
        preference =
            preferenceScreen.findPreference("notification_settings_default")
        if (preference != null) {
            setupNotificationSettingsPreference(preference, Medicine.NotificationImportance.DEFAULT)
        }
        preferenceScreen.findPreference<Preference>(OVERRIDE_DND)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                if (true == value) {
                    showDndPermissions()
                }
                true
            }
        preferenceScreen.findPreference<Preference>(CANNOT_SKIP_REMINDERS)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                if (true == value && preferencesDataSource.preferences.value.dismissNotificationAction == DismissNotificationAction.SKIP) {
                    preferencesDataSource.putString(DISMISS_NOTIFICATION_ACTION, "1")
                    findPreference<ListPreference>(DISMISS_NOTIFICATION_ACTION)?.value = "1"
                }
                true
            }
    }


    private fun setupNotificationSettingsPreference(
        preference: Preference,
        notificationImportance: Medicine.NotificationImportance
    ) {
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, notificationImportance.value.toString())
            startActivity(intent)
            true
        }
    }


    private fun setupExactReminders() {
        val preference =
            preferenceScreen.findPreference<Preference?>(EXACT_REMINDERS) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (true == newValue) {
                        showExactReminderDialog()
                    }
                    true
                }
        } else {
            preference.isVisible = false
        }
    }

    @SuppressLint("BatteryLife")
    private fun setupBatteryOptimization() {
        val preference = preferenceScreen.findPreference<Preference?>("ignore_battery_optimization")

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
        if (!alarmManager.canScheduleExactAlarms()) {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setMessage(com.futsch1.medtimer.core.ui.R.string.enable_alarm_dialog)
            builder.setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                safeStartActivity(context, intent)
            }
            builder.setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { _, _ ->
                try {
                    resetBooleanPreferenceAndReload(EXACT_REMINDERS)
                } catch (_: IllegalStateException) {
                    // Intentionally empty (monkey test can cause this to fail)
                }
            }
            val d = builder.create()
            d.show()
        }
    }

    private fun resetBooleanPreferenceAndReload(preferenceName: String) {
        preferenceManager.preferenceDataStore?.putBoolean(preferenceName, false)
        findPreference<SwitchPreferenceCompat>(preferenceName)?.isChecked = false
    }

    private fun cancelOverrideDnd() {
        try {
            resetBooleanPreferenceAndReload(OVERRIDE_DND)
        } catch (_: IllegalStateException) {
            // Intentionally empty (monkey test can cause this to fail)
        }
    }


    private fun showDndPermissions() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setMessage(com.futsch1.medtimer.core.ui.R.string.enable_dnd_dialog)
            builder.setPositiveButton(com.futsch1.medtimer.core.ui.R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }

            builder.setNegativeButton(com.futsch1.medtimer.core.ui.R.string.cancel) { _, _ -> cancelOverrideDnd() }
            builder.show()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeExactReminders()
        resumeOverrideDnd()
    }

    private fun resumeExactReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            resetBooleanPreferenceAndReload(EXACT_REMINDERS)
        }
    }

    private fun resumeOverrideDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            resetBooleanPreferenceAndReload(OVERRIDE_DND)
        }
    }
}
