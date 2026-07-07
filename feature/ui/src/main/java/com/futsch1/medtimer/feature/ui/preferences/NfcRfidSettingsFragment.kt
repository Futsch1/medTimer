package com.futsch1.medtimer.feature.ui.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.nfc.NfcActionActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NfcRfidSettingsFragment : PreferencesFragment() {
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = preferencesDataSource

        setPreferencesFromResource(R.xml.nfc_rfid_settings, rootKey)

        findPreference<EditTextPreference>("prescription_pickup_days")!!.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val takeScheduledLink = findPreference<Preference>("nfc_take_scheduled_link")!!
        takeScheduledLink.summary = NfcActionActivity.buildTakeScheduledUri().toString()
        takeScheduledLink.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            copyLinkToClipboard(NfcActionActivity.buildTakeScheduledUri().toString())
            true
        }
    }

    private fun copyLinkToClipboard(link: String) {
        val clipboardManager = requireContext().getSystemService<ClipboardManager>() ?: return
        clipboardManager.setPrimaryClip(ClipData.newPlainText(link, link))
        Toast.makeText(context, com.futsch1.medtimer.core.ui.R.string.nfc_link_copied, Toast.LENGTH_SHORT).show()
    }
}
