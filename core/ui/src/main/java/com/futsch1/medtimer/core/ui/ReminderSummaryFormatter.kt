package com.futsch1.medtimer.core.ui

import android.content.Context

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.isReminderActive
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
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
        val instructions = reminder.instructions
        if (instructions?.isNotEmpty() == true) {
            strings.add(instructions)
        }
        strings = strings.filter { it.isNotEmpty() }.toMutableList()

        return join(", ", strings)
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
                timeFormatter.toDateTimeString(reminder.intervalStart)
            )
        }
    }

    private suspend fun linkedReminderString(reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = reminderRepository.fetch(reminder.linkedReminderId) ?: return "?"

        while (current.reminderType == ReminderType.LINKED) {
            delays.add(timeFormatter.toTimeString(current.time))
            current = reminderRepository.fetch(current.linkedReminderId) ?: return "?"
        }

        val base = context.getString(
            R.string.linked_reminder_summary,
            timeFormatter.toTimeString(current.time)
        )
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
    }

    private fun timeBasedReminderString(
        reminder: Reminder,
        strings: MutableList<String>
    ) {
        val dayOfMonthLimited = reminder.activeDaysOfMonth.isNotEmpty() && reminder.activeDaysOfMonth.size != 31
        val hasWeekdayRestriction = reminder.days.isNotEmpty() && reminder.days.size != 7

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

    private fun firstToLower(string: String): String {
        return string.take(1).lowercase(Locale.getDefault()) + string.substring(1)
    }

    private data class ReminderProperties(
        val weekdayLimited: Boolean,
        val dayOfMonthLimited: Boolean
    )
}
