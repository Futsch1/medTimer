package com.futsch1.medtimer.medicine.advancedReminderPreferences

import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import com.futsch1.medtimer.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.helpers.TimeFormatter
import java.time.LocalDate
import javax.inject.Inject

class DateEditHandler @Inject constructor(
    private val datePickerDialogFactory: DatePickerDialogFactory,
    private val timeFormatter: TimeFormatter
) {
    fun show(activity: FragmentActivity, preference: Preference) {
        val currentDateString = preference.preferenceDataStore?.getString(preference.key, null) ?: return

        val currentDate = if (currentDateString != timeFormatter.daysSinceEpochToDateString(0)) {
            timeFormatter.stringToLocalDate(currentDateString)
        } else {
            LocalDate.now()
        }

        datePickerDialogFactory.create(currentDate) { daysSinceEpoch: Long ->
            val newDateString = timeFormatter.daysSinceEpochToDateString(daysSinceEpoch)
            preference.preferenceDataStore?.putString(preference.key, newDateString)
        }.show(activity.supportFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
    }
}