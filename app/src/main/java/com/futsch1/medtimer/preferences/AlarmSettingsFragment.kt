package com.futsch1.medtimer.preferences

import android.os.Bundle
import com.futsch1.medtimer.R
import com.takisoft.preferencex.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.alarm_settings, rootKey)
    }
}