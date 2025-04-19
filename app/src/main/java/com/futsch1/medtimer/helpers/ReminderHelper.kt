package com.futsch1.medtimer.helpers

import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDate

fun isReminderActive(reminder: Reminder): Boolean {
    var active = reminder.active
    if (reminder.periodStart != 0L) {
        active = active && LocalDate.now().toEpochDay() >= reminder.periodStart
    }
    if (reminder.periodEnd != 0L) {
        active = active && LocalDate.now().toEpochDay() <= reminder.periodEnd
    }
    return active
}

fun formatReminderString(
    context: Context,
    reminderEvent: ReminderEvent,
    sharedPreferences: SharedPreferences
): Spanned {
    val takenTime = TimeHelper.toConfigurableDateTimeString(
        context,
        sharedPreferences,
        reminderEvent.remindedTimestamp
    )

    return SpannableStringBuilder().bold { append(reminderEvent.medicineName) }
        .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "")
        .append("\n").append(takenTime)
}

fun formatReminderStringForWidget(
    context: Context,
    reminderEvent: ReminderEvent,
    sharedPreferences: SharedPreferences
): Spanned {
    val takenTime = TimeHelper.toConfigurableDateTimeString(
        context,
        sharedPreferences,
        reminderEvent.remindedTimestamp
    ) + ": "

    return SpannableStringBuilder().append(takenTime).bold { append(reminderEvent.medicineName) }
        .append(" (")
        .append(reminderEvent.amount).append(statusToString(context, reminderEvent.status))
        .append(")")
}

private fun statusToString(context: Context, status: ReminderEvent.ReminderStatus?): String {
    return when (status) {
        ReminderEvent.ReminderStatus.TAKEN -> " " + context.getString(R.string.taken)
        ReminderEvent.ReminderStatus.SKIPPED -> " " + context.getString(R.string.skipped)
        else -> ""
    }
}

fun formatScheduledReminderString(
    context: Context,
    scheduledReminder: ScheduledReminder,
    sharedPreferences: SharedPreferences
): Spanned {
    val scheduledTime = TimeHelper.toConfigurableDateTimeString(
        context,
        sharedPreferences,
        scheduledReminder.timestamp().toEpochMilli() / 1000
    )

    return SpannableStringBuilder().bold { append(scheduledReminder.medicine().medicine.name) }
        .append(if (scheduledReminder.reminder().amount.isNotEmpty()) " (${scheduledReminder.reminder().amount})" else "")
        .append("\n").append(scheduledTime)
}

fun formatScheduledReminderStringForWidget(
    context: Context,
    scheduledReminder: ScheduledReminder,
    sharedPreferences: SharedPreferences
): Spanned {
    val scheduledTime = TimeHelper.toConfigurableDateTimeString(
        context,
        sharedPreferences,
        scheduledReminder.timestamp().toEpochMilli() / 1000
    ) + ": "

    return SpannableStringBuilder().append(scheduledTime)
        .bold { append(scheduledReminder.medicine().medicine.name) }
        .append(if (scheduledReminder.reminder().amount.isNotEmpty()) " (${scheduledReminder.reminder().amount})" else "")
}
