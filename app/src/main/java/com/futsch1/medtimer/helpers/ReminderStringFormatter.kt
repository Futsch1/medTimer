package com.futsch1.medtimer.helpers

import android.content.Context
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderStringFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeFormatter: TimeFormatter
) {
    fun formatReminderEvent(reminderEvent: ReminderEvent): Spanned {
        var takenTime = TimeHelper.secondsSinceEpochToConfigurableTimeString(
            context, preferencesDataSource, reminderEvent.remindedTimestamp, false
        )
        val reminderTypeSpan = getReminderTypeSpan(reminderEvent.reminderType)
        if (reminderEvent.processedTimestamp != 0L && (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN || reminderEvent.status == ReminderEvent.ReminderStatus.ACKNOWLEDGED) &&
            preferencesDataSource.preferences.value.showTakenTimeInOverview
        ) {
            val processedTime = if (TimeHelper.isSameDay(reminderEvent.remindedTimestamp, reminderEvent.processedTimestamp))
                TimeHelper.secondsSinceEpochToConfigurableTimeString(
                    context, preferencesDataSource, reminderEvent.processedTimestamp, false
                )
            else
                TimeHelper.secondsSinceEpochToConfigurableDateTimeString(
                    context, preferencesDataSource, reminderEvent.processedTimestamp
                )

            takenTime = "$takenTime ➡ $processedTime"
        }

        val intervalTime = getLastIntervalTime(reminderEvent)

        return SpannableStringBuilder().append(reminderTypeSpan).append(takenTime).append(intervalTime).append("\n").bold { append(reminderEvent.medicineName) }
            .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "")
    }

    fun formatScheduledReminder(scheduledReminder: ScheduledReminder): Spanned {
        val scheduledTime = TimeHelper.secondsSinceEpochToConfigurableTimeString(
            context, preferencesDataSource, scheduledReminder.timestamp.toEpochMilli() / 1000, false
        )
        val reminderTypeSpan = getReminderTypeSpan(scheduledReminder.reminder.reminderType)

        return SpannableStringBuilder().append(scheduledTime).append("\n").append(reminderTypeSpan).bold {
            append(scheduledReminder.medicine.medicine.name)
        }.append(getAmountOrStockString(scheduledReminder))
    }

    fun formatReminderForWidget(reminderEvent: ReminderEvent, isShort: Boolean): Spanned {
        val takenTime = (if (isShort)
            TimeHelper.secondsSinceEpochToConfigurableTimeString(
                context, preferencesDataSource, reminderEvent.remindedTimestamp, true
            )
        else
            TimeHelper.secondsSinceEpochToConfigurableDateTimeString(
                context, preferencesDataSource, reminderEvent.remindedTimestamp
            )) + ": "
        val reminderTypeSpan = getReminderTypeSpan(reminderEvent.reminderType)

        val amountStatusString = "${reminderEvent.amount} ${statusToString(reminderEvent.status)}".trim()

        return SpannableStringBuilder().append(reminderTypeSpan).append(takenTime).bold { append(reminderEvent.medicineName) }
            .append(if (amountStatusString.isNotEmpty()) " ($amountStatusString)" else "")
    }

    fun formatScheduledReminderForWidget(scheduledReminder: ScheduledReminder, isShort: Boolean): Spanned {
        val scheduledTime = (if (isShort)
            TimeHelper.secondsSinceEpochToConfigurableTimeString(
                context, preferencesDataSource, scheduledReminder.timestamp.toEpochMilli() / 1000, true
            )
        else
            TimeHelper.secondsSinceEpochToConfigurableDateTimeString(
                context, preferencesDataSource, scheduledReminder.timestamp.toEpochMilli() / 1000
            )) + ": "
        val reminderTypeSpan = getReminderTypeSpan(scheduledReminder.reminder.reminderType)

        return SpannableStringBuilder().append(reminderTypeSpan).append(scheduledTime).bold { append(scheduledReminder.medicine.medicine.name) }.append(
            getAmountOrStockString(scheduledReminder)
        )
    }

    fun getReminderTypeSpan(reminderType: Reminder.ReminderType): Spanned {
        val span = SpannableStringBuilder()
        val drawable = ContextCompat.getDrawable(context, reminderType.icon)

        if (drawable != null) {
            val imageSpan = TintedImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

            span.append("  ")
            span.setSpan(imageSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        return span
    }

    private fun statusToString(status: ReminderEvent.ReminderStatus?): String {
        return when (status) {
            ReminderEvent.ReminderStatus.TAKEN -> context.getString(R.string.taken)
            ReminderEvent.ReminderStatus.SKIPPED -> context.getString(R.string.skipped)
            else -> ""
        }
    }

    private fun getAmountOrStockString(scheduledReminder: ScheduledReminder): String {
        val amount =
            when (scheduledReminder.reminder.reminderType) {
                Reminder.ReminderType.OUT_OF_STOCK -> {
                    MedicineHelper.formatAmount(scheduledReminder.medicine.medicine.amount, scheduledReminder.medicine.medicine.unit)
                }

                Reminder.ReminderType.EXPIRATION_DATE -> {
                    timeFormatter.daysSinceEpochToDateString(scheduledReminder.medicine.medicine.expirationDate)
                }

                else -> {
                    scheduledReminder.reminder.amount
                }
            }
        return if (amount.isNotEmpty()) " (${amount})" else ""
    }

    private fun getLastIntervalTime(reminderEvent: ReminderEvent): String =
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

        val measures: MutableList<Measure> = mutableListOf()
        if (hours > 0) measures.add(Measure(hours, MeasureUnit.HOUR))
        if (minutes >= 0) measures.add(Measure(minutes, MeasureUnit.MINUTE))

        val formatter = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
        return formatter.formatMeasures(*measures.toTypedArray<Measure>())
    }
}
