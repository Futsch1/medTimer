package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import android.app.NotificationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.futsch1.medtimer.core.common.helpers.DatePickerDialogFactory
import com.futsch1.medtimer.core.common.helpers.TimePickerDialogFactory
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.Interval
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.ui.R
import com.futsch1.medtimer.feature.ui.medicine.LinkedReminderHandling
import com.futsch1.medtimer.feature.ui.showEnableFullScreenIntentDialog
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalDate.EPOCH
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
    listOf(
        "instructions",
        "interval_start_time",
        "interval_daily_start_time",
        "interval_daily_end_time"
    )
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

    @Inject
    lateinit var notificationManager: NotificationManager

    @Suppress("kotlin:S6530") // False positive
    override val customOnClick: Map<String, (FragmentActivity, Preference) -> Unit>
        get() = mapOf(
            "add_linked_reminder" to { activity, preference ->
                val reminderDataStore = preference.preferenceDataStore as ReminderDataStore
                linkedReminderHandlingFactory.create(
                    reminderDataStore.modelData,
                    activity.lifecycleScope
                ).addLinkedReminder(activity)
            },
            "interval_start_time" to { activity, preference ->
                showDateTimeEdit(
                    activity,
                    preference
                )
            },
            "interval_daily_start_time" to { activity, preference ->
                showTimeEdit(
                    activity,
                    preference
                )
            },
            "interval_daily_end_time" to { activity, preference ->
                showTimeEdit(
                    activity,
                    preference
                )
            }
        )

    val menuProvider by lazy { menuProviderFactory.create(this) }

    override fun onModelDataUpdated(modelData: Reminder) {
        super.onModelDataUpdated(modelData)

        menuProvider.reminder = modelData

        findPreference<Preference>("reminder_status")?.summary = getDateRangeSummary(modelData)
        findPreference<Preference>("interval")?.summary =
            Interval(modelData.time).toTranslatedString(requireContext())
        findPreference<Preference>("remind_on_weekdays")?.summary = getWeekdaysSummary(modelData)
        findPreference<Preference>("remind_on_days")?.summary = getDaysSummary(modelData)
        findPreference<Preference>("interval_start")?.summary =
            requireContext().getString(if (modelData.intervalStartsFromProcessed) com.futsch1.medtimer.core.ui.R.string.interval_start_processed else com.futsch1.medtimer.core.ui.R.string.interval_start_reminded)
        findPreference<Preference>("interval_type")?.summary =
            reminderSummaryFormatter.getIntervalTypeSummary(modelData)
        findPreference<Preference>("cyclic_reminder")?.summary =
            reminderSummaryFormatter.getCyclicReminderString(modelData)
        findPreference<Preference>("notification_importance")?.summary =
            when (modelData.notificationImportance) {
                Reminder.NotificationImportance.SAME_AS_MEDICINE -> getString(com.futsch1.medtimer.core.ui.R.string.same_as_medicine)
                Reminder.NotificationImportance.DEFAULT -> getString(com.futsch1.medtimer.core.ui.R.string.default_)
                Reminder.NotificationImportance.HIGH -> getString(com.futsch1.medtimer.core.ui.R.string.high)
                Reminder.NotificationImportance.HIGH_AND_ALARM -> getString(com.futsch1.medtimer.core.ui.R.string.high_and_alarm)
            }
    }

    private fun getDaysSummary(reminder: Reminder): String {
        return if (reminder.activeDaysOfMonth.isEmpty() || reminder.activeDaysOfMonth.size == 31) {
            requireContext().getString(com.futsch1.medtimer.core.ui.R.string.every_day_of_month)
        } else {
            requireContext().getString(
                com.futsch1.medtimer.core.ui.R.string.on_day_of_month,
                reminder.activeDaysOfMonth.joinToString(", ")
            )
        }
    }

    private fun getWeekdaysSummary(reminder: Reminder): String {
        return if (reminder.days.isEmpty() || reminder.days.size == 7) {
            requireContext().getString(com.futsch1.medtimer.core.ui.R.string.every_day)
        } else {
            reminder.days.joinToString(", ") {
                it.getDisplayName(
                    TextStyle.FULL,
                    requireContext().resources.configuration.locales[0]
                )
            }
        }
    }

    override fun customSetup(modelData: Reminder) {
        findPreference<Preference>("add_linked_reminder")?.isVisible = !modelData.isInterval
        findPreference<Preference>("interval_category")?.isVisible = modelData.isInterval
        findPreference<Preference>("interval_start_time")?.isVisible =
            modelData.reminderType == ReminderType.CONTINUOUS_INTERVAL
        findPreference<Preference>("interval_daily_start_time")?.isVisible =
            modelData.reminderType == ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("interval_daily_end_time")?.isVisible =
            modelData.reminderType == ReminderType.WINDOWED_INTERVAL
        findPreference<Preference>("time_based_category")?.isVisible =
            modelData.reminderType == ReminderType.TIME_BASED

        findPreference<Preference>("interval")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { _ ->
                EditIntervalDialog(requireContext(), modelData) { newIntervalMinutes ->
                    preferenceManager.preferenceDataStore?.putInt(
                        "interval",
                        newIntervalMinutes
                    )
                }
                true
            }

        findPreference<ListPreference>("notification_importance")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == "3") {
                    showEnableFullScreenIntentDialog(requireContext(), notificationManager)
                }
                true
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

    private fun getDateRangeSummary(modelData: Reminder): String {
        val hasStart = modelData.periodStart != EPOCH
        val hasEnd = modelData.periodEnd != EPOCH
        return when {
            hasStart && hasEnd -> requireContext().getString(
                com.futsch1.medtimer.core.ui.R.string.active_date_range_range,
                timeFormatter.localDateToString(modelData.periodStart),
                timeFormatter.localDateToString(modelData.periodEnd)
            )
            hasStart -> requireContext().getString(
                com.futsch1.medtimer.core.ui.R.string.active_date_range_from,
                timeFormatter.localDateToString(modelData.periodStart)
            )
            hasEnd -> requireContext().getString(
                com.futsch1.medtimer.core.ui.R.string.active_date_range_until,
                timeFormatter.localDateToString(modelData.periodEnd)
            )
            else -> requireContext().getString(com.futsch1.medtimer.core.ui.R.string.active_date_range_no_limit)
        }
    }

    private fun showDateTimeEdit(activity: FragmentActivity, preference: Preference) {
        val currentDateString =
            preference.preferenceDataStore?.getString(preference.key, null) ?: return
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