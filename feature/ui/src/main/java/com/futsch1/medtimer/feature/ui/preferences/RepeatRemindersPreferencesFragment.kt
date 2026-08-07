package com.futsch1.medtimer.feature.ui.preferences

import android.os.Bundle
import com.futsch1.medtimer.core.ui.preferences.MedTimerPreferenceFragment
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.ui.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RepeatRemindersPreferencesFragment : MedTimerPreferenceFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.repeat_reminders_preferences, rootKey)
    }
}
