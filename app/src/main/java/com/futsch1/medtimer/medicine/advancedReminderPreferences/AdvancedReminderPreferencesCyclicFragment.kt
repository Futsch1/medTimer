package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.text.InputType
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEntity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedReminderPreferencesCyclicFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_cyclic,
    mapOf(
    ),
    mapOf(
    ),
    listOf("cycle_start_date", "cycle_consecutive_days", "cycle_pause_days")
) {
    @Inject
    lateinit var dateEditHandler: DateEditHandler

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "cycle_start_date" to { activity, preference -> dateEditHandler.show(activity, preference) },
        )

    override fun customSetup(entity: ReminderEntity) {
        findPreference<EditTextPreference>("cycle_consecutive_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        findPreference<EditTextPreference>("cycle_pause_days")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
    }
}