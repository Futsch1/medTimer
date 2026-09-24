package com.futsch1.medtimer.feature.ui.preferences

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.ui.component.withTopAppBar
import com.futsch1.medtimer.feature.ui.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            RingtonePreferenceDialogFragment.RESULT,
            this
        ) { _, result ->
            val key = result.getString(RingtonePreferenceDialogFragment.KEY_PREFERENCE)
                ?: return@setFragmentResultListener
            val preference = preferenceManager.findPreference<RingtonePreference>(key)
                ?: return@setFragmentResultListener
            preference.setRingtone(
                result.getString(RingtonePreferenceDialogFragment.KEY_URI)?.let(Uri::parse)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = withTopAppBar(super.onCreateView(inflater, container, savedInstanceState))

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.alarm_settings, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is RingtonePreference) {
            RingtonePreferenceDialogFragment.newInstance(preference).show(
                parentFragmentManager,
                preference.key
            )
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}
