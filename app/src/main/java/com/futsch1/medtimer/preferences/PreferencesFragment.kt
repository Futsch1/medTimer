package com.futsch1.medtimer.preferences

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.Navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.R

fun setupPreferencesLink(
    preferencesFragment: PreferencesFragment,
    preferenceKey: String,
    @IdRes actionId: Int
) {
    val preference = preferencesFragment.findPreference<Preference?>(preferenceKey)
    preference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
        val navController = findNavController(preferencesFragment.requireView())
        try {
            navController.navigate(actionId)
        } catch (_: IllegalArgumentException) {
            // Intentionally empty (monkey test can cause this to fail)
        }
        true
    }
}

open class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        setupTheme()
        setupPreferencesLink(
            this,
            "notification_settings",
            R.id.action_preferencesFragment_to_notificationSettingsFragment
        )
        setupPreferencesLink(
            this,
            "display_settings",
            R.id.action_preferencesFragment_to_displaySettingsFragment
        )
        setupPreferencesLink(
            this,
            "weekend_mode",
            R.id.action_preferencesFragment_to_weekendModePreferencesFragment
        )
        setupPreferencesLink(
            this,
            "privacy_settings",
            R.id.action_preferencesFragment_to_privacyPreferencesFragment
        )
    }

    private fun setupTheme() {
        val preference = preferenceScreen.findPreference<Preference?>("theme")
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                try {
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    requireActivity().finish()
                    startActivity(intent)
                    return@OnPreferenceChangeListener true
                } catch (_: IllegalStateException) {
                    return@OnPreferenceChangeListener false
                }
            }
    }

    override fun onResume() {
        super.onResume()
        resumeExactReminders()
        resumeOverrideDnd()
    }

    private fun resumeExactReminders() {
        val preference =
            preferenceScreen.findPreference<SwitchPreferenceCompat?>(PreferencesNames.EXACT_REMINDERS)
        if (preference != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager =
                requireContext().getSystemService<AlarmManager>(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                preference.setChecked(false)
            }
        }
    }

    private fun resumeOverrideDnd() {
        val preference =
            preferenceScreen.findPreference<SwitchPreferenceCompat?>(PreferencesNames.OVERRIDE_DND)
        if (preference != null && !requireContext().getSystemService<NotificationManager?>(
                NotificationManager::class.java
            ).isNotificationPolicyAccessGranted()
        ) {
            preference.setChecked(false)
        }
    }
}