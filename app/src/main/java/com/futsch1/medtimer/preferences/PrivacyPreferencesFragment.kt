package com.futsch1.medtimer.preferences

import android.os.Bundle
import android.view.WindowManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.futsch1.medtimer.Biometrics
import com.futsch1.medtimer.R
import com.futsch1.medtimer.preferences.PreferencesNames.SECURE_WINDOW

class PrivacyPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.privacy_preferences, rootKey)
        setupBlockScreenCapture()
        setupAppAuthentication()
    }

    private fun setupBlockScreenCapture() {
        val preference = preferenceScreen.findPreference<SwitchPreference>(SECURE_WINDOW)
        if (preference != null) {
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                    requireActivity().window.setFlags(
                        if (java.lang.Boolean.TRUE == newValue) WindowManager.LayoutParams.FLAG_SECURE else 0,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                    true
                }
        }
    }

    private fun setupAppAuthentication() {
        val preference = preferenceScreen.findPreference<Preference>("app_authentication")
        if (preference != null) {
            preference.isEnabled = Biometrics(this.requireActivity(), {}, {}).hasBiometrics()
        }
    }

}