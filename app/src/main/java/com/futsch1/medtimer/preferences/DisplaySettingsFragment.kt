package com.futsch1.medtimer.preferences

import android.os.Bundle
import com.futsch1.medtimer.R

class DisplaySettingsFragment : PreferencesFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.display_settings, rootKey)
    }
}