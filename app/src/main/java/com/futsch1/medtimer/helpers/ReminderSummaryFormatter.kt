package com.futsch1.medtimer.helpers

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderSummaryFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter
) {
    suspend fun formatExportReminderSummary(reminder: ReminderEntity): String {
        val reminderSummary = formatReminderSummary(reminder)
        if (!reminder.usesTimeInMinutes) {
            return reminderSummary
        }

        val time: String = timeFormatter.minutesToTimeString(reminder.timeInMinutes)
        return "$time, $reminderSummary"
    }

    suspend fun formatReminderSummary(reminder: ReminderEntity): String {
        var strings: MutableList<String> = mutableListOf()

        if (!isReminderActive(reminder)) {
            strings.add(context.getString(R.string.inactive))
        }
        when (reminder.reminderType) {
            ReminderEntity.ReminderType.LINKED -> {
                strings.add(linkedReminderString(reminder))
            }

            ReminderEntity.ReminderType.CONTINUOUS_INTERVAL, ReminderEntity.ReminderType.WINDOWED_INTERVAL -> {
                strings.add(intervalBasedReminderString(reminder))
            }

            ReminderEntity.ReminderType.TIME_BASED -> {
                timeBasedReminderString(reminder, strings)
            }

            ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                strings.add(outOfStockReminderString(reminder))
            }

            ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                strings.add(expirationDateReminderString(reminder))
            }

            ReminderEntity.ReminderType.REFILL -> {
                strings.add(context.getString(R.string.refill))
            }
        }
        if (reminder.instructions?.isNotEmpty() == true) {
            strings.add(reminder.instructions!!)
        }
        strings = strings.filter { it.isNotEmpty() }.toMutableList()

        return java.lang.String.join(", ", strings)
    }

    suspend fun formatRemindersSummary(reminders: List<ReminderEntity>): String {
        val reminderTimes = buildList {
            addAll(timeBasedRemindersSummary(reminders.filter { it.reminderType == ReminderEntity.ReminderType.TIME_BASED }))
            addAll(reminders.filter { it.reminderType == ReminderEntity.ReminderType.LINKED }.map { linkedReminderSummaryString(it) })
            addAll(reminders.filter { it.isInterval }.map { intervalBasedReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderEntity.ReminderType.OUT_OF_STOCK }.map { outOfStockReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderEntity.ReminderType.EXPIRATION_DATE }.map { context.getString(R.string.expiration_date) })
        }

        val len = reminderTimes.size
        return context.resources.getQuantityString(
            R.plurals.sum_reminders,
            len,
            len,
            java.lang.String.join("; ", reminderTimes)
        )
    }

    private fun expirationDateReminderString(reminder: ReminderEntity): String {
        return context.getString(R.string.expiration_date) + ", " +
                context.resources.getStringArray(R.array.expiration_reminder)[reminder.expirationReminderType.ordinal]
    }

    private fun outOfStockReminderString(reminder: ReminderEntity): String {
        return context.resources.getStringArray(R.array.stock_reminder)[reminder.outOfStockReminderType.ordinal] + " " + MedicineHelper.formatAmount(
            reminder.outOfStockThreshold,
            ""
        )
    }

    private fun intervalBasedReminderString(reminder: ReminderEntity): String {
        val interval = Interval(reminder.timeInMinutes)
        return context.getString(
            R.string.every_interval,
            interval.toTranslatedString(context)
        ) + ", " + getIntervalTypeSummary(reminder)
    }

    fun getIntervalTypeSummary(reminder: ReminderEntity): String {
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

    private suspend fun linkedReminderString(reminder: ReminderEntity): String {
        val delays = mutableListOf<String>()
        var current = reminderRepository.get(reminder.linkedReminderId) ?: return "?"

        while (current.reminderType == ReminderEntity.ReminderType.LINKED) {
            delays.add(timeFormatter.minutesToDurationString(current.timeInMinutes))
            current = reminderRepository.get(current.linkedReminderId) ?: return "?"
        }

        val base = context.getString(
            R.string.linked_reminder_summary,
            timeFormatter.minutesToTimeString(current.timeInMinutes)
        )
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private suspend fun linkedReminderSummaryString(reminder: ReminderEntity): String {
        val delays = mutableListOf<String>()
        var current = reminder

        while (current.reminderType == ReminderEntity.ReminderType.LINKED) {
            delays.add(timeFormatter.minutesToDurationString(current.timeInMinutes))
            val source = reminderRepository.get(current.linkedReminderId) ?: return "?"
            current = source
        }

        val base = timeFormatter.minutesToTimeString(current.timeInMinutes)
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private fun timeBasedReminderString(
        reminder: ReminderEntity,
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
        reminder: ReminderEntity,
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

    fun getCyclicReminderString(reminder: ReminderEntity): String {
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

    private fun timeBasedRemindersSummary(reminders: List<ReminderEntity>): List<String> {
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
