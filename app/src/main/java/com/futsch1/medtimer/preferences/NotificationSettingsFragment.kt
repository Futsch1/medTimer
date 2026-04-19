package com.futsch1.medtimer.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.safeStartActivity
import com.futsch1.medtimer.location.GeofenceRegistrar
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.EXACT_REMINDERS
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.LOCATION_SNOOZE_ENABLED
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.OVERRIDE_DND
import com.futsch1.medtimer.preferences.PreferencesDataSource.Companion.STICKY_ON_LOCKSCREEN
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

    @Inject
    lateinit var geofenceRegistrar: GeofenceRegistrar

    @Inject
    lateinit var homeLocationDataSource: HomeLocationDataSource

    private lateinit var requestFineLocationLauncher: ActivityResultLauncher<String>
    private lateinit var requestBackgroundLocationLauncher: ActivityResultLauncher<String>

    private var rootKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFineLocationLauncher = registerForActivityResult(RequestPermission()) { granted ->
            when {
                granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> showBackgroundLocationRationale()
                granted -> onLocationPermissionsGranted()
                else -> resetBooleanPreferenceAndReload(LOCATION_SNOOZE_ENABLED)
            }
        }
        requestBackgroundLocationLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                onLocationPermissionsGranted()
            } else {
                resetBooleanPreferenceAndReload(LOCATION_SNOOZE_ENABLED)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        this.rootKey = rootKey

        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.notification_settings, rootKey)

        setupNotificationSettings()
        setupExactReminders()
        setupBatteryOptimization()
        setupLocationSnooze()

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
        preference =
            preferenceScreen.findPreference(OVERRIDE_DND)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                if (true == value) {
                    showDndPermissions()
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

    private fun setupLocationSnooze() {
        val preference = preferenceScreen.findPreference<SwitchPreferenceCompat>(LOCATION_SNOOZE_ENABLED) ?: return
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                if (!geofenceRegistrar.isLocationServiceAvailable()) {
                    showLocationSnoozeInfoDialog(R.string.location_snooze_no_play_services)
                    return@OnPreferenceChangeListener false
                }
                if (homeLocationDataSource.getHomeLocation() == null) {
                    showLocationSnoozeInfoDialog(R.string.location_snooze_no_home_location)
                    return@OnPreferenceChangeListener false
                }
                requestFineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                geofenceRegistrar.unregisterHomeGeofence()
            }
            true
        }
    }

    private fun showLocationSnoozeInfoDialog(@StringRes messageRes: Int) {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(messageRes)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationRationale() {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.location_snooze_background_permission)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                resetBooleanPreferenceAndReload(LOCATION_SNOOZE_ENABLED)
            }
            .show()
    }

    private fun onLocationPermissionsGranted() {
        if (!geofenceRegistrar.registerHomeGeofence()) {
            resetBooleanPreferenceAndReload(LOCATION_SNOOZE_ENABLED)
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
            builder.setMessage(R.string.enable_alarm_dialog)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                safeStartActivity(context, intent)
            }
            builder.setNegativeButton(R.string.cancel) { _, _ ->
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
            builder.setMessage(R.string.enable_dnd_dialog)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }

            builder.setNegativeButton(R.string.cancel) { _, _ -> cancelOverrideDnd() }
            builder.show()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeExactReminders()
        resumeOverrideDnd()
        resumeLocationSnooze()
    }

    private fun resumeExactReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                resetBooleanPreferenceAndReload(EXACT_REMINDERS)
            }
        }
    }

    private fun resumeOverrideDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            resetBooleanPreferenceAndReload(OVERRIDE_DND)
        }
    }

    private fun resumeLocationSnooze() {
        val pref = findPreference<SwitchPreferenceCompat>(LOCATION_SNOOZE_ENABLED) ?: return
        if (pref.isChecked && !geofenceRegistrar.hasRequiredPermissions()) {
            resetBooleanPreferenceAndReload(LOCATION_SNOOZE_ENABLED)
            geofenceRegistrar.unregisterHomeGeofence()
        }
    }
}
