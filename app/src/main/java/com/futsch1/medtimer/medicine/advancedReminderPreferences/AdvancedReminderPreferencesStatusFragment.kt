package com.futsch1.medtimer.medicine.advancedReminderPreferences

import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedReminderPreferencesStatusFragment : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_status,
    mapOf(
    ),
    mapOf(
    ),
    listOf("period_start_date", "period_end_date")
) {
    @Inject
    lateinit var dateEditHandler: DateEditHandler

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "period_start_date" to { activity, preference -> dateEditHandler.show(activity, preference) },
            "period_end_date" to { activity, preference -> dateEditHandler.show(activity, preference) }
        )

    override fun onEntityUpdated(entity: Reminder) {
        super.onEntityUpdated(entity)

        findPreference<Preference>("period_start_date")?.isVisible = entity.periodStart != 0L
        findPreference<Preference>("period_end_date")?.isVisible = entity.periodEnd != 0L
    }

}