package com.futsch1.medtimer.feature.ui.impl.preferences

import android.os.Bundle
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.ui.impl.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DisplaySettingsFragment : PreferencesFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.display_settings, rootKey)
    }
}