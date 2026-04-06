package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.String.join
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderSummaryFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter
) {
    suspend fun formatExportReminderSummary(reminder: Reminder): String {
        val reminderSummary = formatReminderSummary(reminder)
        if (!reminder.usesTimeInMinutes) {
            return reminderSummary
        }

        val time: String = timeFormatter.toTimeString(reminder.time)
        return "$time, $reminderSummary"
    }

    suspend fun formatReminderSummary(reminder: Reminder): String {
        var strings: MutableList<String> = mutableListOf()

        if (!isReminderActive(reminder)) {
            strings.add(context.getString(R.string.inactive))
        }
        when (reminder.reminderType) {
            ReminderType.LINKED -> {
                strings.add(linkedReminderString(reminder))
            }

            ReminderType.CONTINUOUS_INTERVAL, ReminderType.WINDOWED_INTERVAL -> {
                strings.add(intervalBasedReminderString(reminder))
            }

            ReminderType.TIME_BASED -> {
                timeBasedReminderString(reminder, strings)
            }

            ReminderType.OUT_OF_STOCK -> {
                strings.add(outOfStockReminderString(reminder))
            }

            ReminderType.EXPIRATION_DATE -> {
                strings.add(expirationDateReminderString(reminder))
            }

            ReminderType.REFILL -> {
                strings.add(context.getString(R.string.refill))
            }
        }
        if (reminder.instructions?.isNotEmpty() == true) {
            strings.add(reminder.instructions)
        }
        strings = strings.filter { it.isNotEmpty() }.toMutableList()

        return join(", ", strings)
    }

    suspend fun formatRemindersSummary(reminders: List<Reminder>): String {
        val reminderTimes = buildList {
            addAll(timeBasedRemindersSummary(reminders.filter { it.reminderType == ReminderType.TIME_BASED }))
            addAll(reminders.filter { it.reminderType == ReminderType.LINKED }.map { linkedReminderSummaryString(it) })
            addAll(reminders.filter { it.isInterval }.map { intervalBasedReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderType.OUT_OF_STOCK }.map { outOfStockReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderType.EXPIRATION_DATE }.map { context.getString(R.string.expiration_date) })
        }

        val len = reminderTimes.size
        return context.resources.getQuantityString(
            R.plurals.sum_reminders,
            len,
            len,
            join("; ", reminderTimes)
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
        val interval = Interval(reminder.time)
        return context.getString(
            R.string.every_interval,
            interval.toTranslatedString(context)
        ) + ", " + getIntervalTypeSummary(reminder)
    }

    fun getIntervalTypeSummary(reminder: Reminder): String {
        return if (reminder.windowedInterval) {
            context.getString(
                R.string.daily_from_to,
                timeFormatter.toTimeString(reminder.intervalStartTimeOfDay),
                timeFormatter.toTimeString(reminder.intervalEndTimeOfDay)
            )
        } else {
            context.getString(
                R.string.continuous_from,
                timeFormatter.secondsSinceEpochToDateTimeString(reminder.intervalStart.epochSecond)
            )
        }
    }

    private suspend fun linkedReminderString(reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = reminderRepository.get(reminder.linkedReminderId) ?: return "?"

        while (current.reminderType == ReminderType.LINKED) {
            delays.add(timeFormatter.toTimeString(current.time))
            current = reminderRepository.get(current.linkedReminderId) ?: return "?"
        }

        val base = context.getString(
            R.string.linked_reminder_summary,
            timeFormatter.toTimeString(current.time)
        )
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private suspend fun linkedReminderSummaryString(reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = reminder

        while (current.reminderType == ReminderType.LINKED) {
            delays.add(timeFormatter.toTimeString(current.time))
            val source = reminderRepository.get(current.linkedReminderId) ?: return "?"
            current = source
        }

        val base = timeFormatter.toTimeString(current.time)
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private fun timeBasedReminderString(
        reminder: Reminder,
        strings: MutableList<String>
    ) {
        val dayOfMonthLimited = reminder.activeDaysOfMonth.isNotEmpty()
        val hasWeekdayRestriction = reminder.days.isNotEmpty()

        buildReminderStrings(
            strings,
            reminder,
            ReminderProperties(hasWeekdayRestriction, dayOfMonthLimited)
        )
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
                timeFormatter.daysSinceEpochToDateString(reminder.cycleStartDay.toEpochDay())
        else ""
    }

    private fun timeBasedRemindersSummary(reminders: List<Reminder>): List<String> {
        return reminders.map { r -> r.time }.sorted()
            .map { timeFormatter.toTimeString(it) }
    }

    private fun firstToLower(string: String): String {
        return string.take(1).lowercase(Locale.getDefault()) + string.substring(1)
    }

    private data class ReminderProperties(
        val weekdayLimited: Boolean,
        val dayOfMonthLimited: Boolean
    )
}
