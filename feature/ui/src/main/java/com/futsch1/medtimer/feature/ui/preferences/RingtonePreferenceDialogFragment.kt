package com.futsch1.medtimer.feature.ui.preferences

import android.app.Dialog
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class RingtonePreferenceDialogFragment : DialogFragment() {
    private var selectedIndex = -1
    private var ringtoneUris = emptyList<Uri?>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ringtoneType = requireArguments().getInt(KEY_TYPE)
        val showDefault = requireArguments().getBoolean(KEY_SHOW_DEFAULT)
        val showSilent = requireArguments().getBoolean(KEY_SHOW_SILENT)
        val defaultUri = RingtoneManager.getDefaultUri(ringtoneType)
        val currentUri = requireArguments().getString(KEY_URI)?.let(Uri::parse)
        val manager = RingtoneManager(requireContext()).apply {
            setType(ringtoneType)
        }
        val names = mutableListOf<String>()
        ringtoneUris = buildList {
            if (showDefault) {
                add(defaultUri)
                names += RingtoneManager.getRingtone(requireContext(), defaultUri)?.getTitle(requireContext())
                    ?: getString(android.R.string.untitled)
            }
            if (showSilent) {
                add(null)
                names += getString(android.R.string.untitled)
            }
            manager.cursor.use { cursor ->
                while (cursor.moveToNext()) {
                    add(manager.getRingtoneUri(cursor.position))
                    names += cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                }
            }
        }

        selectedIndex = ringtoneUris.indexOfFirst { it == currentUri }
        if (selectedIndex < 0 && currentUri != null) {
            val managerPosition = try {
                manager.getRingtonePosition(currentUri)
            } catch (_: NumberFormatException) {
                -1
            }
            if (managerPosition >= 0) {
                selectedIndex = managerPosition +
                        (if (showDefault) 1 else 0) +
                        (if (showSilent) 1 else 0)
            } else if (currentUri == defaultUri && showDefault) {
                selectedIndex = 0
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(requireArguments().getCharSequence(KEY_TITLE))
            .setSingleChoiceItems(names.toTypedArray(), selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    RESULT,
                    Bundle().apply {
                        putString(KEY_PREFERENCE, requireArguments().getString(KEY_PREFERENCE))
                        putString(KEY_URI, ringtoneUris.getOrNull(selectedIndex)?.toString())
                    }
                )
            }
            .create()
    }

    companion object {
        const val RESULT = "ringtone_preference_result"
        const val KEY_PREFERENCE = "preference_key"
        const val KEY_URI = "uri"
        private const val KEY_TYPE = "type"
        private const val KEY_SHOW_DEFAULT = "show_default"
        private const val KEY_SHOW_SILENT = "show_silent"
        private const val KEY_TITLE = "title"

        fun newInstance(preference: RingtonePreference) = RingtonePreferenceDialogFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_PREFERENCE, preference.key)
                putString(KEY_URI, preference.ringtone?.toString())
                putInt(KEY_TYPE, preference.ringtoneType)
                putBoolean(KEY_SHOW_DEFAULT, preference.showDefault)
                putBoolean(KEY_SHOW_SILENT, preference.showSilent)
                putCharSequence(KEY_TITLE, preference.dialogTitle)
            }
        }
    }
}
