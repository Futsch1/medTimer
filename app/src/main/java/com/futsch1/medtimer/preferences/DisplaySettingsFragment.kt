package com.futsch1.medtimer.preferences

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.TimeHelper

class DisplaySettingsFragment : PreferencesFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.display_settings, rootKey)

        val systemLocalePreference = findPreference<SwitchPreferenceCompat>("system_locale")
        systemLocalePreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            TimeHelper.onChangedUseSystemLocale()
            true
        }
    }
}