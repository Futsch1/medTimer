package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderSummaryFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter
) {
    suspend fun formatExportReminderSummary(reminder: Reminder): String {
        val reminderSummary = formatReminderSummary(reminder)
        if (!reminder.usesTimeInMinutes) {
            return reminderSummary
        }

        val time: String = timeFormatter.minutesToTimeString(reminder.timeInMinutes)
        return "$time, $reminderSummary"
    }

    suspend fun formatReminderSummary(reminder: Reminder): String {
        var strings: MutableList<String> = mutableListOf()

        if (!isReminderActive(reminder)) {
            strings.add(context.getString(R.string.inactive))
        }
        when (reminder.reminderType) {
            Reminder.ReminderType.LINKED -> {
                strings.add(linkedReminderString(reminder))
            }

            Reminder.ReminderType.CONTINUOUS_INTERVAL, Reminder.ReminderType.WINDOWED_INTERVAL -> {
                strings.add(intervalBasedReminderString(reminder))
            }

            Reminder.ReminderType.TIME_BASED -> {
                timeBasedReminderString(reminder, strings)
            }

            Reminder.ReminderType.OUT_OF_STOCK -> {
                strings.add(outOfStockReminderString(reminder))
            }

            Reminder.ReminderType.EXPIRATION_DATE -> {
                strings.add(expirationDateReminderString(reminder))
            }

            Reminder.ReminderType.REFILL -> {
                strings.add(context.getString(R.string.refill))
            }
        }
        if (reminder.instructions?.isNotEmpty() == true) {
            strings.add(reminder.instructions!!)
        }
        strings = strings.filter { it.isNotEmpty() }.toMutableList()

        return java.lang.String.join(", ", strings)
    }

    suspend fun formatRemindersSummary(reminders: List<Reminder>): String {
        val reminderTimes = buildList {
            addAll(timeBasedRemindersSummary(reminders.filter { it.reminderType == Reminder.ReminderType.TIME_BASED }))
            addAll(reminders.filter { it.reminderType == Reminder.ReminderType.LINKED }.map { linkedReminderSummaryString(it) })
            addAll(reminders.filter { it.isInterval }.map { intervalBasedReminderString(it) })
            addAll(reminders.filter { it.reminderType == Reminder.ReminderType.OUT_OF_STOCK }.map { outOfStockReminderString(it) })
            addAll(reminders.filter { it.reminderType == Reminder.ReminderType.EXPIRATION_DATE }.map { context.getString(R.string.expiration_date) })
        }

        val len = reminderTimes.size
        return context.resources.getQuantityString(
            R.plurals.sum_reminders,
            len,
            len,
            java.lang.String.join("; ", reminderTimes)
        )
    }

    private fun expirationDateReminderString(reminder: Reminder): String {
        return context.getString(R.string.expiration_date) + ", " +
                context.resources.getStringArray(R.array.expiration_reminder)[reminder.expirationReminderType.ordinal]
    }

    private fun outOfStockReminderString(reminder: Reminder): String {
        return context.resources.getStringArray(R.array.stock_reminder)[reminder.outOfStockReminderType.ordinal] + " " + MedicineHelper.formatAmount(
            reminder.outOfStockThreshold,
            ""
        )
    }

    private fun intervalBasedReminderString(reminder: Reminder): String {
        val interval = Interval(reminder.timeInMinutes)
        return context.getString(
            R.string.every_interval,
            interval.toTranslatedString(context)
        ) + ", " + getIntervalTypeSummary(reminder)
    }

    fun getIntervalTypeSummary(reminder: Reminder): String {
        return if (reminder.windowedInterval) {
            context.getString(
                R.string.daily_from_to,
                timeFormatter.minutesToTimeString(reminder.intervalStartTimeOfDay),
                timeFormatter.minutesToTimeString(reminder.intervalEndTimeOfDay)
            )
        } else {
            context.getString(
                R.string.continuous_from,
                timeFormatter.secondsSinceEpochToDateTimeString(reminder.intervalStart)
            )
        }
    }

    private suspend fun linkedReminderString(reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = medicineRepository.getReminder(reminder.linkedReminderId) ?: return "?"

        while (current.reminderType == Reminder.ReminderType.LINKED) {
            delays.add(timeFormatter.minutesToDurationString(current.timeInMinutes))
            current = medicineRepository.getReminder(current.linkedReminderId) ?: return "?"
        }

        val base = context.getString(
            R.string.linked_reminder_summary,
            timeFormatter.minutesToTimeString(current.timeInMinutes)
        )
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private suspend fun linkedReminderSummaryString(reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = reminder

        while (current.reminderType == Reminder.ReminderType.LINKED) {
            delays.add(timeFormatter.minutesToDurationString(current.timeInMinutes))
            val source = medicineRepository.getReminder(current.linkedReminderId) ?: return "?"
            current = source
        }

        val base = timeFormatter.minutesToTimeString(current.timeInMinutes)
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private fun timeBasedReminderString(
        reminder: Reminder,
        strings: MutableList<String>
    ) {
        val never = reminder.days.none { it } || (reminder.activeDaysOfMonth and 0x7FFFFFFF) == 0
        if (never) {
            strings.add(context.getString(R.string.never))
        } else {
            val dayOfMonthLimited = (reminder.activeDaysOfMonth and 0x7FFFFFFF) != 0x7FFFFFFF
            val hasWeekdayRestriction = reminder.days.any { !it }

            buildReminderStrings(
                strings,
                reminder,
                ReminderProperties(hasWeekdayRestriction, dayOfMonthLimited)
            )
        }
    }

    private fun buildReminderStrings(
        strings: MutableList<String>,
        reminder: Reminder,
        properties: ReminderProperties
    ) {
        if (properties.weekdayLimited) {
            strings.add(context.getString(R.string.weekday_limited))
        }
        if (properties.dayOfMonthLimited) {
            strings.add(context.getString(R.string.day_of_month_limited))
        }
        strings.add(getCyclicReminderString(reminder))
        if (!properties.weekdayLimited && reminder.pauseDays == 0) {
            strings.add(context.getString(R.string.every_day))
        }
    }

    fun getCyclicReminderString(reminder: Reminder): String {
        return if (reminder.pauseDays > 0) context.getString(R.string.cycle_reminder) +
                " " +
                reminder.consecutiveDays +
                "/" +
                reminder.pauseDays +
                ", " +
                firstToLower(context.getString(R.string.cycle_start_date)) +
                " " +
                timeFormatter.daysSinceEpochToDateString(reminder.cycleStartDay)
        else ""
    }

    private fun timeBasedRemindersSummary(reminders: List<Reminder>): List<String> {
        return reminders.map { r -> r.timeInMinutes }.sorted()
            .map { timeFormatter.minutesToTimeString(it) }
    }

    private fun firstToLower(string: String): String {
        return string.take(1).lowercase(Locale.getDefault()) + string.substring(1)
    }

    private data class ReminderProperties(
        val weekdayLimited: Boolean,
        val dayOfMonthLimited: Boolean
    )
}
