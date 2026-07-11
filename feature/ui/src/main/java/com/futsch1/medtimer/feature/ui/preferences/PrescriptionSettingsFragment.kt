package com.futsch1.medtimer.feature.ui.preferences

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.ui.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrescriptionSettingsFragment : PreferencesFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.prescription_settings, rootKey)

        findPreference<EditTextPreference>("prescription_contact_global")!!.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_PHONE
        }
        findPreference<EditTextPreference>("prescription_message_template")!!.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            editText.setSingleLine(false)
        }
        findPreference<EditTextPreference>("prescription_pickup_days")!!.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}
