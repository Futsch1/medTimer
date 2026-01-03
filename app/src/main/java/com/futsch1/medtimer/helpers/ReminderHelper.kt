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
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDate
import java.util.Locale
import java.util.stream.Collectors


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

fun getActiveReminders(medicine: FullMedicine): List<Reminder> {
    return medicine.reminders.stream().filter { isReminderActive(it) }.collect(Collectors.toList())
}

fun setRemindersActive(reminders: List<Reminder>, medicineRepository: MedicineRepository, active: Boolean) {
    for (reminder in reminders) {
        setReminderActive(reminder, active)
        medicineRepository.updateReminder(reminder)
    }
}

fun setAllRemindersActive(medicine: FullMedicine, medicineRepository: MedicineRepository, active: Boolean) {
    setRemindersActive(medicine.reminders, medicineRepository, active)
}

fun setReminderActive(reminder: Reminder, active: Boolean) {
    if (!reminder.active && active && reminder.reminderType == Reminder.ReminderType.CONTINUOUS_INTERVAL) {
        // If reminder is activated again and an interval reminder, reset the interval start date to the current day in seconds since epoch
        reminder.intervalStart = TimeHelper.changeTimeStampDate(reminder.intervalStart, LocalDate.now())
    }
    reminder.active = active
}

fun formatReminderString(
    context: Context, reminderEvent: ReminderEvent, sharedPreferences: SharedPreferences
): Spanned {
    val takenTime = TimeHelper.secondsSinceEpochToConfigurableTimeString(
        context, sharedPreferences, reminderEvent.remindedTimestamp, false
    )

    val intervalTime = getLastIntervalTime(context, reminderEvent)

    return SpannableStringBuilder().append(takenTime).append(intervalTime).append("\n").bold { append(reminderEvent.medicineName) }
        .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "")
}

fun formatReminderStringForWidget(
    context: Context, reminderEvent: ReminderEvent, sharedPreferences: SharedPreferences, isShort: Boolean
): Spanned {
    val takenTime = (if (isShort)
        TimeHelper.secondsSinceEpochToConfigurableTimeString(
            context, sharedPreferences, reminderEvent.remindedTimestamp, true
        )
    else
        TimeHelper.secondsSinceEpochToConfigurableDateTimeString(
            context, sharedPreferences, reminderEvent.remindedTimestamp
        )) + ": "

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
    val scheduledTime = TimeHelper.secondsSinceEpochToConfigurableTimeString(
        context, sharedPreferences, scheduledReminder.timestamp().toEpochMilli() / 1000, false
    )

    return SpannableStringBuilder().append(scheduledTime).append("\n").bold {
        append(scheduledReminder.medicine().medicine.name)
    }.append(getAmountString(scheduledReminder))
}

fun formatScheduledReminderStringForWidget(
    context: Context, scheduledReminder: ScheduledReminder, sharedPreferences: SharedPreferences, isShort: Boolean
): Spanned {
    val scheduledTime = (if (isShort)
        TimeHelper.secondsSinceEpochToConfigurableTimeString(
            context, sharedPreferences, scheduledReminder.timestamp().toEpochMilli() / 1000, true
        )
    else
        TimeHelper.secondsSinceEpochToConfigurableDateTimeString(
            context, sharedPreferences, scheduledReminder.timestamp().toEpochMilli() / 1000
        )) + ": "

    return SpannableStringBuilder().append(scheduledTime).bold { append(scheduledReminder.medicine().medicine.name) }.append(getAmountString(scheduledReminder))
}

private fun getAmountString(scheduledReminder: ScheduledReminder): String =
    if (scheduledReminder.reminder().amount.isNotEmpty()) " (${scheduledReminder.reminder().amount})" else ""


private fun getLastIntervalTime(context: Context, reminderEvent: ReminderEvent): String =
    if (reminderEvent.lastIntervalReminderTimeInMinutes > 0 && reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN && calcLastIntervalTime(reminderEvent) >= 0) {
        " (" + context.getString(
            R.string.interval_time, formatDuration(
                calcLastIntervalTime(reminderEvent)
            ).toString()
        ) + ")"
    } else {
        ""
    }

private fun calcLastIntervalTime(reminderEvent: ReminderEvent): Long =
    reminderEvent.processedTimestamp * 1000L - reminderEvent.lastIntervalReminderTimeInMinutes * 60_000L

private fun formatDuration(durationMillis: Long): String? {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    val measures: MutableList<Measure?> = ArrayList()
    if (hours > 0) measures.add(Measure(hours, MeasureUnit.HOUR))
    if (minutes >= 0) measures.add(Measure(minutes, MeasureUnit.MINUTE))

    val formatter = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
    return formatter.formatMeasures(*measures.toTypedArray<Measure?>())
}
