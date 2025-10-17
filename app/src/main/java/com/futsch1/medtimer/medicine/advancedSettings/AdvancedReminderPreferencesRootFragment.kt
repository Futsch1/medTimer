package com.futsch1.medtimer.medicine.advancedSettings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.TimeHelper.DatePickerWrapper
import com.futsch1.medtimer.helpers.getCyclicReminderString
import com.futsch1.medtimer.helpers.getIntervalTypeSummary
import com.futsch1.medtimer.helpers.isReminderActive
import com.futsch1.medtimer.medicine.LinkedReminderHandling
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId


fun showDateEdit(activity: FragmentActivity, preference: Preference) {
    val datePickerWrapper = DatePickerWrapper(activity)
    val currentDateString = preference.preferenceDataStore?.getString(preference.key, null)
    if (currentDateString != null) {
        val currentDate = TimeHelper.dateStringToDate(activity, currentDateString)
        datePickerWrapper.show(currentDate) { daysSinceEpoch: Long ->
            val newDateString = TimeHelper.daysSinceEpochToDateString(activity, daysSinceEpoch)
            preference.preferenceDataStore?.putString(preference.key, newDateString)
        }
    }
}

fun showTimeEdit(activity: FragmentActivity, preference: Preference) {
    val timePickerWrapper = TimeHelper.TimePickerWrapper(activity)
    val currentTimeString = preference.preferenceDataStore?.getString(preference.key, null)
    if (currentTimeString != null) {
        val currentTime = TimeHelper.timeStringToMinutes(activity, currentTimeString)
        timePickerWrapper.show(currentTime / 60, currentTime % 60) { minutes: Int ->
            val newTimeString = TimeHelper.minutesToTimeString(activity, minutes.toLong())
            preference.preferenceDataStore?.putString(preference.key, newTimeString)
        }
    }
}

fun showDateTimeEdit(activity: FragmentActivity, preference: Preference) {
    val datePickerWrapper = DatePickerWrapper(activity)
    val currentDateString = preference.preferenceDataStore?.getString(preference.key, null)
    if (currentDateString != null) {
        val currentDateTime = TimeHelper.dateTimeStringToSecondsSinceEpoch(activity, currentDateString)
        val startInstant = Instant.ofEpochSecond(currentDateTime)
        val dateTime = startInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        datePickerWrapper.show(dateTime.toLocalDate()) { daysSinceEpoch: Long ->
            val timePickerWrapper = TimeHelper.TimePickerWrapper(activity)
            timePickerWrapper.show(dateTime.hour, dateTime.minute) { minutes: Int ->
                val selectedLocalDateTime = LocalDateTime.of(
                    LocalDate.ofEpochDay(daysSinceEpoch),
                    LocalTime.of(minutes / 60, minutes % 60)
                )
                val timeString = TimeHelper.toLocalizedDatetimeString(activity, selectedLocalDateTime)
                preference.preferenceDataStore?.putString(preference.key, timeString)
            }
        }
    }
}

class AdvancedReminderPreferencesRootFragment(
) : AdvancedReminderPreferencesFragment(
    R.xml.advanced_reminder_settings_root,
    mapOf(
        "instructions" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesInstructionsFragment(
                id
            )
        },
        "reminder_status" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesStatusFragment(
                id
            )
        },
        "cyclic_reminder" to { id ->
            AdvancedReminderPreferencesRootFragmentDirections.actionAdvancedReminderPreferencesRootFragmentToAdvancedReminderPreferencesCyclicFragment(
                id
            )
        }
    ),
    mapOf(
        "add_linked_reminder" to { activity, preference ->
            val reminderDataStore = preference.preferenceDataStore as ReminderDataStore
            LinkedReminderHandling(reminderDataStore.reminder, reminderDataStore.medicineRepository, activity.lifecycleScope).addLinkedReminder(activity)
        },
        "interval_start_time" to { activity, preference -> showDateTimeEdit(activity, preference) },
        "interval_daily_start_time" to { activity, preference -> showTimeEdit(activity, preference) },
        "interval_daily_end_time" to { activity, preference -> showTimeEdit(activity, preference) }
    ),
    listOf("instructions", "interval_start_time", "interval_daily_start_time", "interval_daily_end_time")
) {
    override fun onReminderUpdated(reminder: Reminder) {
        super.onReminderUpdated(reminder)

        findPreference<Preference>("reminder_status")?.summary =
            requireContext().getString(if (isReminderActive(reminder)) R.string.active else R.string.inactive)
        findPreference<Preference>("interval")?.summary = Interval(reminder.timeInMinutes).toTranslatedString(requireContext())
        findPreference<Preference>("remind_on_weekdays")?.summary = getWeekdaysSummary(reminder)
        findPreference<Preference>("remind_on_days")?.summary = getDaysSummary(reminder)
        findPreference<Preference>("interval_start")?.summary =
            requireContext().getString(if (reminder.intervalStartsFromProcessed) R.string.interval_start_processed else R.string.interval_start_reminded)
        findPreference<Preference>("interval_type")?.summary = getIntervalTypeSummary(reminder, requireContext())
        findPreference<Preference>("cyclic_reminder")?.summary = getCyclicReminderString(reminder, requireContext())
    }

    private fun getDaysSummary(reminder: Reminder): String {
        return if ((reminder.activeDaysOfMonth and 0x7FFF_FFFF) == 0x7FFF_FFFF) {
            requireContext().getString(R.string.every_day_of_month)
        } else {
            val days: MutableList<String> = mutableListOf()
            for (i in 0..30) {
                if ((reminder.activeDaysOfMonth and (1 shl i)) != 0) {
                    days += (i + 1).toString()
                }
            }
            requireContext().getString(R.string.on_day_of_month, days.joinToString(", "))
        }
    }

    private fun getWeekdaysSummary(reminder: Reminder): String {
        return if (reminder.days.none { it }) {
            requireContext().getString(R.string.never)
        } else if (reminder.days.all { it }) {
            requireContext().getString(R.string.every_day)
        } else {
            val days: MutableList<String> = mutableListOf()
            for ((i, day) in requireContext().resources.getStringArray(R.array.days).withIndex()) {
                if (reminder.days[i]) {
                    days += day
                }
            }
            days.joinToString(", ")
        }
    }

    override fun customSetup(reminder: Reminder) {
        findPreference<Preference>("interval_category")?.isVisible =
            reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL || reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("interval_start_time")?.isVisible = reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL
        findPreference<Preference>("interval_daily_start_time")?.isVisible = reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("interval_daily_end_time")?.isVisible = reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("time_based_category")?.isVisible = reminder.reminderType == Reminder.ReminderType.TIME_BASED

        findPreference<Preference>("interval")?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            EditIntervalDialog(requireContext(), reminder.timeInMinutes) { newIntervalMinutes ->
                preferenceManager.preferenceDataStore?.putInt(
                    "interval",
                    newIntervalMinutes
                )
            }
            true
        }

        requireActivity().addMenuProvider(
            AdvancedReminderSettingsMenuProvider(reminder, (preferenceManager.preferenceDataStore as ReminderDataStore).medicineRepository, this),
            getViewLifecycleOwner()
        )
    }

}