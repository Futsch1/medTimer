package com.futsch1.medtimer.feature.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.ui.component.withTopAppBar
import com.futsch1.medtimer.feature.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Uses the preferencex base rather than MedTimerPreferenceFragment because it needs onCreatePreferencesFix.
@AndroidEntryPoint
class AlarmSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = withTopAppBar(super.onCreateView(inflater, container, savedInstanceState))

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.alarm_settings, rootKey)
    }
}