package com.futsch1.medtimer.preferences

import android.os.Bundle
import com.futsch1.medtimer.R
import com.takisoft.preferencex.PreferenceFragmentCompat

class AlarmSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.alarm_settings, rootKey)
    }
}