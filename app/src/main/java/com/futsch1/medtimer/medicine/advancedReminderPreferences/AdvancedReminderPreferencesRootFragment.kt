package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.helpers.isReminderActive
import com.futsch1.medtimer.medicine.LinkedReminderHandling
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import javax.inject.Inject


@AndroidEntryPoint
class AdvancedReminderPreferencesRootFragment : AdvancedReminderPreferencesFragment(
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
    mapOf(),
    listOf("instructions", "interval_start_time", "interval_daily_start_time", "interval_daily_end_time")
) {
    @Inject
    lateinit var menuProviderFactory: AdvancedReminderSettingsMenuProvider.Factory

    @Inject
    lateinit var linkedReminderHandlingFactory: LinkedReminderHandling.Factory

    @Inject
    lateinit var timePickerDialogFactory: TimePickerDialogFactory

    @Inject
    lateinit var datePickerDialogFactory: DatePickerDialogFactory

    @Inject
    lateinit var reminderSummaryFormatter: ReminderSummaryFormatter

    @Inject
    lateinit var timeFormatter: TimeFormatter

    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "add_linked_reminder" to { activity, preference ->
                val reminderDataStore = preference.preferenceDataStore as ReminderDataStore
                linkedReminderHandlingFactory.create(reminderDataStore.modelData, activity.lifecycleScope).addLinkedReminder(activity)
            },
            "interval_start_time" to { activity, preference -> showDateTimeEdit(activity, preference) },
            "interval_daily_start_time" to { activity, preference -> showTimeEdit(activity, preference) },
            "interval_daily_end_time" to { activity, preference -> showTimeEdit(activity, preference) }
        )

    val menuProvider by lazy { menuProviderFactory.create(this) }

    override fun onModelDataUpdated(modelData: Reminder) {
        super.onModelDataUpdated(modelData)

        menuProvider.reminder = modelData

        findPreference<Preference>("reminder_status")?.summary =
            requireContext().getString(if (isReminderActive(modelData)) R.string.active else R.string.inactive)
        findPreference<Preference>("interval")?.summary = Interval(modelData.time).toTranslatedString(requireContext())
        findPreference<Preference>("remind_on_weekdays")?.summary = getWeekdaysSummary(modelData)
        findPreference<Preference>("remind_on_days")?.summary = getDaysSummary(modelData)
        findPreference<Preference>("interval_start")?.summary =
            requireContext().getString(if (modelData.intervalStartsFromProcessed) R.string.interval_start_processed else R.string.interval_start_reminded)
        findPreference<Preference>("interval_type")?.summary = reminderSummaryFormatter.getIntervalTypeSummary(modelData)
        findPreference<Preference>("cyclic_reminder")?.summary = reminderSummaryFormatter.getCyclicReminderString(modelData)
    }

    private fun getDaysSummary(reminder: Reminder): String {
        return if (reminder.activeDaysOfMonth.isEmpty() || reminder.activeDaysOfMonth.size == 31) {
            requireContext().getString(R.string.every_day_of_month)
        } else {
            requireContext().getString(R.string.on_day_of_month, reminder.activeDaysOfMonth.joinToString(", "))
        }
    }

    private fun getWeekdaysSummary(reminder: Reminder): String {
        return if (reminder.days.isEmpty() || reminder.days.size == 7) {
            requireContext().getString(R.string.every_day)
        } else {
            reminder.days.joinToString(", ") { it.getDisplayName(TextStyle.FULL, requireContext().resources.configuration.locales[0]) }
        }
    }

    override fun customSetup(modelData: Reminder) {
        findPreference<Preference>("add_linked_reminder")?.isVisible = !modelData.isInterval
        findPreference<Preference>("interval_category")?.isVisible = modelData.isInterval
        findPreference<Preference>("interval_start_time")?.isVisible = modelData.reminderType == ReminderType.CONTINUOUS_INTERVAL
        findPreference<Preference>("interval_daily_start_time")?.isVisible = modelData.reminderType == ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("interval_daily_end_time")?.isVisible = modelData.reminderType == ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("time_based_category")?.isVisible = modelData.reminderType == ReminderType.TIME_BASED

        findPreference<Preference>("interval")?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            EditIntervalDialog(requireContext(), modelData) { newIntervalMinutes ->
                preferenceManager.preferenceDataStore?.putInt(
                    "interval",
                    newIntervalMinutes
                )
            }
            true
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        requireActivity().addMenuProvider(
            menuProvider,
            getViewLifecycleOwner()
        )

        return view
    }

    private fun showTimeEdit(activity: FragmentActivity, preference: Preference) {
        val currentTimeString = preference.preferenceDataStore?.getString(preference.key, null)
        if (currentTimeString != null) {
            val currentTime = timeFormatter.timeStringToMinutes(currentTimeString)
            timePickerDialogFactory.create(currentTime / 60, currentTime % 60) { minutes: Int ->
                val newTimeString = timeFormatter.minutesToTimeString(minutes)
                preference.preferenceDataStore?.putString(preference.key, newTimeString)
            }.show(activity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
        }
    }

    private fun showDateTimeEdit(activity: FragmentActivity, preference: Preference) {
        val currentDateString = preference.preferenceDataStore?.getString(preference.key, null) ?: return
        val currentDateTime = timeFormatter.stringToInstant(currentDateString)
        val startInstant = currentDateTime ?: return
        val dateTime = startInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        datePickerDialogFactory.create(dateTime.toLocalDate()) { daysSinceEpoch: Long ->
            timePickerDialogFactory.create(dateTime.toLocalTime()) { minutes: Int ->
                val selectedLocalDateTime = LocalDateTime.of(
                    LocalDate.ofEpochDay(daysSinceEpoch),
                    LocalTime.of(minutes / 60, minutes % 60)
                )
                val timeString = timeFormatter.toDateTimeString(selectedLocalDateTime)
                preference.preferenceDataStore?.putString(preference.key, timeString)
            }.show(activity.supportFragmentManager, TimePickerDialogFactory.DIALOG_TAG)
        }.show(activity.supportFragmentManager, DatePickerDialogFactory.DIALOG_TAG)
    }

}