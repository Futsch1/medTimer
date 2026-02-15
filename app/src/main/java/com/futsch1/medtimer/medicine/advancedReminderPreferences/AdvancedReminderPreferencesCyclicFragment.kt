package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.text.InputType
import androidx.preference.EditTextPreference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder

class AdvancedReminderPreferencesCyclicFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_cyclic,
    mapOf(
    ),
    mapOf(
        "cycle_start_date" to { activity, preference -> showDateEdit(activity, preference) },
    ),
    listOf("cycle_start_date", "cycle_consecutive_days", "cycle_pause_days")
) {

    override fun customSetup(entity: Reminder) {
        findPreference<EditTextPreference>("cycle_consecutive_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        findPreference<EditTextPreference>("cycle_pause_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
    }
}