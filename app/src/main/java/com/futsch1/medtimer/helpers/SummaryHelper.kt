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

fun reminderSummary(context: Context, reminder: Reminder): String {
    val strings: MutableList<String> = LinkedList()

    if (!isReminderActive(reminder)) {
        strings.add(context.getString(R.string.inactive))
    }
    if (reminder.linkedReminderId != 0) {
        strings.add(linkedReminderString(reminder, context))
    } else {
        standardReminderString(reminder, strings, context)
    }
    if (reminder.instructions != null && reminder.instructions.isNotEmpty()) {
        strings.add(reminder.instructions)
    }

    return java.lang.String.join(", ", strings)
}

fun linkedReminderString(reminder: Reminder, context: Context): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val sourceReminder = medicineRepository.getReminder(reminder.linkedReminderId)

    if (sourceReminder != null) {
        return if (sourceReminder.linkedReminderId != 0) {
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

private fun standardReminderString(
    reminder: Reminder,
    strings: MutableList<String>,
    context: Context
) {
    val weekdayLimited =
        !reminder.days.stream().allMatch { day: Boolean -> day }
    val dayOfMonthLimited = (reminder.activeDaysOfMonth and 0x7FFFFFFF) != 0x7FFFFFFF
    val never = reminder.days.stream()
        .noneMatch { day: Boolean -> day } || (reminder.activeDaysOfMonth and 0x7FFFFFFF) == 0
    val cyclic = reminder.pauseDays > 0
    if (never) {
        strings.add(context.getString(R.string.never))
    } else {
        buildReminderStrings(
            context,
            strings,
            reminder,
            ReminderProperties(weekdayLimited, dayOfMonthLimited, cyclic)
        )
    }
}

data class ReminderProperties(
    val weekdayLimited: Boolean,
    val dayOfMonthLimited: Boolean,
    val cyclic: Boolean
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
    if (properties.cyclic) {
        strings.add(getCyclicReminderString(context, reminder))
    }
    if (!properties.weekdayLimited && !properties.cyclic) {
        strings.add(context.getString(R.string.every_day))
    }
}

fun remindersSummary(context: Context, reminders: List<Reminder>): String {
    val reminderTimes = standardRemindersSummary(
        reminders.stream().filter { r: Reminder -> r.linkedReminderId == 0 }, context
    ) + linkedRemindersSummary(
        reminders.stream().filter { r: Reminder -> r.linkedReminderId != 0 }, context
    )

    val len = reminderTimes.size
    return context.resources.getQuantityString(
        R.plurals.sum_reminders,
        len,
        len,
        java.lang.String.join(", ", reminderTimes)
    )

}

fun linkedRemindersSummary(reminders: Stream<Reminder>, context: Context): List<String> {
    return reminders.map { r: Reminder -> linkedReminderSummaryString(r, context) }
        .collect(Collectors.toList())
}

fun linkedReminderSummaryString(reminder: Reminder, context: Context): String {
    val medicineRepository = MedicineRepository(context.applicationContext as Application?)
    val sourceReminder = medicineRepository.getReminder(reminder.linkedReminderId)

    if (sourceReminder != null) {
        return if (sourceReminder.linkedReminderId != 0) {
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

private fun standardRemindersSummary(
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

private fun getCyclicReminderString(context: Context, reminder: Reminder): String {
    return context.getString(R.string.cycle_reminders) +
            " " +
            reminder.consecutiveDays +
            "/" +
            reminder.pauseDays +
            ", " +
            firstToLower(context.getString(R.string.cycle_start_date)) +
            " " +
            TimeHelper.daysSinceEpochToDateString(context, reminder.cycleStartDay)
}

private fun firstToLower(string: String): String {
    return string.substring(0, 1).lowercase(Locale.getDefault()) + string.substring(1)
}