package com.futsch1.medtimer.helpers

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDate
import java.util.Locale


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
    context: Context, reminderEvent: ReminderEvent, sharedPreferences: SharedPreferences
): Spanned {
    val takenTime = TimeHelper.toConfigurableDateTimeString(
        context, sharedPreferences, reminderEvent.remindedTimestamp
    )

    val intervalTime = getLastIntervalTime(context, reminderEvent)

    return SpannableStringBuilder().bold { append(reminderEvent.medicineName) }
        .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "").append("\n").append(takenTime).append(intervalTime)
}

fun formatReminderStringForWidget(
    context: Context, reminderEvent: ReminderEvent, sharedPreferences: SharedPreferences
): Spanned {
    val takenTime = TimeHelper.toConfigurableDateTimeString(
        context, sharedPreferences, reminderEvent.remindedTimestamp
    ) + ": "

    val amountStatusString = "${reminderEvent.amount} ${statusToString(context, reminderEvent.status)}".trim()

    return SpannableStringBuilder().append(takenTime).bold { append(reminderEvent.medicineName) }
        .append(if (amountStatusString.isNotEmpty()) " ($amountStatusString)" else "")
}

private fun statusToString(context: Context, status: ReminderEvent.ReminderStatus?): String {
    return when (status) {
        ReminderEvent.ReminderStatus.TAKEN -> context.getString(R.string.taken)
        ReminderEvent.ReminderStatus.SKIPPED -> context.getString(R.string.skipped)
        else -> ""
    }
}

fun formatScheduledReminderString(
    context: Context, scheduledReminder: ScheduledReminder, sharedPreferences: SharedPreferences
): Spanned {
    val scheduledTime = TimeHelper.toConfigurableDateTimeString(
        context, sharedPreferences, scheduledReminder.timestamp().toEpochMilli() / 1000
    )

    return SpannableStringBuilder().bold { append(scheduledReminder.medicine().medicine.name) }.append(getAmountString(scheduledReminder)).append("\n")
        .append(scheduledTime)
}

fun formatScheduledReminderStringForWidget(
    context: Context, scheduledReminder: ScheduledReminder, sharedPreferences: SharedPreferences
): Spanned {
    val scheduledTime = TimeHelper.toConfigurableDateTimeString(
        context, sharedPreferences, scheduledReminder.timestamp().toEpochMilli() / 1000
    ) + ": "

    return SpannableStringBuilder().append(scheduledTime).bold { append(scheduledReminder.medicine().medicine.name) }.append(getAmountString(scheduledReminder))
}

private fun getAmountString(scheduledReminder: ScheduledReminder): String =
    if (scheduledReminder.reminder().amount.isNotEmpty()) " (${scheduledReminder.reminder().amount})" else ""


private fun getLastIntervalTime(context: Context, reminderEvent: ReminderEvent): String = if (reminderEvent.lastIntervalReminderTimeInMinutes > 0) {
    " (" + context.getString(
        R.string.interval_time, formatDuration(
            reminderEvent.remindedTimestamp * 1000L - reminderEvent.lastIntervalReminderTimeInMinutes * 60_000L
        ).toString()
    ) + ")"
} else {
    ""
}

private fun formatDuration(durationMillis: Long): String? {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val measures: MutableList<Measure?> = ArrayList<Measure?>()
    if (hours > 0) measures.add(Measure(hours, MeasureUnit.HOUR))
    if (minutes > 0) measures.add(Measure(minutes, MeasureUnit.MINUTE))
    if (seconds > 0 && hours == 0L) measures.add(Measure(seconds, MeasureUnit.SECOND)) // skip seconds if showing hours

    val formatter = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
    return formatter.formatMeasures(*measures.toTypedArray<Measure?>())
}
