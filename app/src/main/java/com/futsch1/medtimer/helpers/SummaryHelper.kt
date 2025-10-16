package com.futsch1.medtimer.helpers

import android.app.Application
import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import java.util.LinkedList
import java.util.Locale
import java.util.stream.Collectors
import java.util.stream.Stream

fun reminderSummary(reminder: Reminder, context: Context): String {
    var strings: MutableList<String> = LinkedList()

    if (!isReminderActive(reminder)) {
        strings.add(context.getString(R.string.inactive))
    }
    when (reminder.reminderType) {
        Reminder.ReminderType.LINKED -> {
            strings.add(linkedReminderString(reminder, context))
        }

        Reminder.ReminderType.CONTINUOUS_INTERVAL -> {
            strings.add(intervalBasedReminderString(reminder, context))
        }

        else -> {
            timeBasedReminderString(reminder, strings, context)
        }
    }
    if (reminder.instructions != null && reminder.instructions.isNotEmpty()) {
        strings.add(reminder.instructions)
    }
    strings = strings.filter { it.isNotEmpty() }.toMutableList()

    return java.lang.String.join(", ", strings)
}

fun intervalBasedReminderString(
    reminder: Reminder,
    context: Context
): String {
    val interval = Interval(reminder.timeInMinutes)
    return context.getString(R.string.every_interval, interval.toTranslatedString(context)) + ", " + getIntervalTypeSummary(reminder, context)
}

fun getIntervalTypeSummary(reminder: Reminder, context: Context): String {
    return if (reminder.windowedInterval) {
        context.getString(
            R.string.daily_from_to,
            TimeHelper.minutesToTimeString(context, reminder.intervalStartTimeOfDay.toLong()),
            TimeHelper.minutesToTimeString(context, reminder.intervalEndTimeOfDay.toLong())
        )
    } else {
        context.getString(R.string.continuous_from, TimeHelper.toLocalizedDatetimeString(context, reminder.intervalStart))
    }
}

fun linkedReminderString(reminder: Reminder, context: Context): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val sourceReminder = medicineRepository.getReminder(reminder.linkedReminderId)

    if (sourceReminder != null) {
        return if (sourceReminder.reminderType == Reminder.ReminderType.LINKED) {
            // Recursion
            linkedReminderString(sourceReminder, context) + " + " +
                    TimeHelper.minutesToDurationString(sourceReminder.timeInMinutes.toLong())
        } else {
            context.getString(
                R.string.linked_reminder_summary,
                TimeHelper.minutesToTimeString(context, sourceReminder.timeInMinutes.toLong())
            )
        }
    }
    return "?"
}

private fun timeBasedReminderString(
    reminder: Reminder,
    strings: MutableList<String>,
    context: Context
) {
    val weekdayLimited =
        !reminder.days.stream().allMatch { day: Boolean -> day }
    val dayOfMonthLimited = (reminder.activeDaysOfMonth and 0x7FFFFFFF) != 0x7FFFFFFF
    val never = reminder.days.stream()
        .noneMatch { day: Boolean -> day } || (reminder.activeDaysOfMonth and 0x7FFFFFFF) == 0
    if (never) {
        strings.add(context.getString(R.string.never))
    } else {
        buildReminderStrings(
            context,
            strings,
            reminder,
            ReminderProperties(weekdayLimited, dayOfMonthLimited)
        )
    }
}

data class ReminderProperties(
    val weekdayLimited: Boolean,
    val dayOfMonthLimited: Boolean
)

private fun buildReminderStrings(
    context: Context,
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
    strings.add(getCyclicReminderString(reminder, context))
    if (!properties.weekdayLimited && reminder.pauseDays == 0) {
        strings.add(context.getString(R.string.every_day))
    }
}

fun remindersSummary(reminders: List<Reminder>, context: Context): String {
    val reminderTimes = timeBasedRemindersSummary(
        reminders.stream()
            .filter { r: Reminder -> r.reminderType == Reminder.ReminderType.TIME_BASED },
        context
    ) + getRemindersSummary(
        reminders.stream().filter { r: Reminder -> r.reminderType == Reminder.ReminderType.LINKED },
        ({ r: Reminder -> linkedReminderSummaryString(r, context) })
    ) + getRemindersSummary(
        reminders.stream()
            .filter { r: Reminder -> r.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL || r.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL },
        ({ r: Reminder -> intervalBasedReminderString(r, context) })
    )

    val len = reminderTimes.size
    return context.resources.getQuantityString(
        R.plurals.sum_reminders,
        len,
        len,
        java.lang.String.join("; ", reminderTimes)
    )
}

fun getRemindersSummary(
    reminders: Stream<Reminder>,
    toStringFunction: (Reminder) -> String
): List<String> {
    return reminders.map { r: Reminder -> toStringFunction(r) }
        .collect(Collectors.toList())
}

fun linkedReminderSummaryString(reminder: Reminder, context: Context): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val sourceReminder = medicineRepository.getReminder(reminder.linkedReminderId)

    if (sourceReminder != null) {
        return if (sourceReminder.reminderType == Reminder.ReminderType.LINKED) {
            // Recursion
            linkedReminderSummaryString(
                sourceReminder,
                context
            ) + " + " + TimeHelper.minutesToDurationString(reminder.timeInMinutes.toLong())
        } else {
            TimeHelper.minutesToTimeString(context, sourceReminder.timeInMinutes.toLong()) + " + " +
                    TimeHelper.minutesToDurationString(reminder.timeInMinutes.toLong())

        }
    }
    return "?"
}

private fun timeBasedRemindersSummary(
    reminders: Stream<Reminder>,
    context: Context
): ArrayList<String> {
    val reminderTimes = ArrayList<String>()
    val timesInMinutes =
        reminders.mapToInt { r: Reminder -> r.timeInMinutes }.sorted().toArray()
    for (minute in timesInMinutes) {
        reminderTimes.add(TimeHelper.minutesToTimeString(context, minute.toLong()))
    }
    return reminderTimes
}

fun getCyclicReminderString(reminder: Reminder, context: Context): String {
    return if (reminder.pauseDays > 0) context.getString(R.string.cycle_reminder) +
            " " +
            reminder.consecutiveDays +
            "/" +
            reminder.pauseDays +
            ", " +
            firstToLower(context.getString(R.string.cycle_start_date)) +
            " " +
            TimeHelper.daysSinceEpochToDateString(context, reminder.cycleStartDay)
    else ""
}

private fun firstToLower(string: String): String {
    return string.substring(0, 1).lowercase(Locale.getDefault()) + string.substring(1)
}