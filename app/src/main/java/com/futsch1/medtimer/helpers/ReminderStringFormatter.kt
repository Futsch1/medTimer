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
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.toEntityReminderType
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
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
    fun formatReminderEvent(reminderEvent: ReminderEventEntity): Spanned {
        var takenTime = timeFormatter.secondsSinceEpochToConfigurableTimeString(
            reminderEvent.remindedTimestamp, false
        )
        val reminderTypeSpan = getReminderTypeSpan(reminderEvent.reminderType)
        if (reminderEvent.processedTimestamp != 0L && (reminderEvent.status == ReminderEventEntity.ReminderStatus.TAKEN || reminderEvent.status == ReminderEventEntity.ReminderStatus.ACKNOWLEDGED) &&
            preferencesDataSource.preferences.value.showTakenTimeInOverview
        ) {
            val processedTime = if (TimeHelper.isSameDay(reminderEvent.remindedTimestamp, reminderEvent.processedTimestamp))
                timeFormatter.secondsSinceEpochToConfigurableTimeString(
                    reminderEvent.processedTimestamp, false
                )
            else
                timeFormatter.secondsSinceEpochToConfigurableDateTimeString(
                    reminderEvent.processedTimestamp
                )

            takenTime = "$takenTime ➡ $processedTime"
        }

        val intervalTime = getLastIntervalTime(reminderEvent)

        return SpannableStringBuilder().append(reminderTypeSpan).append(takenTime).append(intervalTime).append("\n").bold { append(reminderEvent.medicineName) }
            .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "")
    }

    fun formatReminderEvent(reminderEvent: ReminderEvent): Spanned {
        var takenTime = timeFormatter.secondsSinceEpochToConfigurableTimeString(
            reminderEvent.remindedTimestamp.epochSecond, false
        )
        val reminderTypeSpan = getReminderTypeSpan(reminderEvent)
        val processedTimestamp = reminderEvent.processedTimestamp.epochSecond
        if (processedTimestamp != 0L && (reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN || reminderEvent.status == ReminderEvent.ReminderStatus.ACKNOWLEDGED) &&
            preferencesDataSource.preferences.value.showTakenTimeInOverview
        ) {
            val remindedTimestamp = reminderEvent.remindedTimestamp.epochSecond
            val processedTime = if (TimeHelper.isSameDay(remindedTimestamp, processedTimestamp))
                timeFormatter.secondsSinceEpochToConfigurableTimeString(processedTimestamp, false)
            else
                timeFormatter.secondsSinceEpochToConfigurableDateTimeString(processedTimestamp)

            takenTime = "$takenTime ➡ $processedTime"
        }

        val intervalTime = getLastIntervalTime(reminderEvent)

        return SpannableStringBuilder().append(reminderTypeSpan).append(takenTime).append(intervalTime).append("\n").bold { append(reminderEvent.medicineName) }
            .append(if (reminderEvent.amount.isNotEmpty()) " (${reminderEvent.amount})" else "")
    }

    fun formatScheduledReminder(scheduledReminder: ScheduledReminder): Spanned {
        val scheduledTime = timeFormatter.secondsSinceEpochToConfigurableTimeString(
            scheduledReminder.timestamp.toEpochMilli() / 1000, false
        )
        val reminderTypeSpan = getReminderTypeSpan(scheduledReminder.reminder.reminderType)

        return SpannableStringBuilder().append(scheduledTime).append("\n").append(reminderTypeSpan).bold {
            append(scheduledReminder.medicine.medicine.name)
        }.append(getAmountOrStockString(scheduledReminder))
    }

    fun formatReminderForWidget(reminderEvent: ReminderEvent, isShort: Boolean): Spanned {
        val takenTime = (if (isShort)
            timeFormatter.secondsSinceEpochToConfigurableTimeString(
                reminderEvent.remindedTimestamp.epochSecond, true
            )
        else
            timeFormatter.secondsSinceEpochToConfigurableDateTimeString(
                reminderEvent.remindedTimestamp.epochSecond
            )) + ": "
        val reminderTypeSpan = getReminderTypeSpan(reminderEvent)

        val amountStatusString = "${reminderEvent.amount} ${statusToString(reminderEvent.status)}".trim()

        return SpannableStringBuilder().append(reminderTypeSpan).append(takenTime).bold { append(reminderEvent.medicineName) }
            .append(if (amountStatusString.isNotEmpty()) " ($amountStatusString)" else "")
    }

    fun formatScheduledReminderForWidget(scheduledReminder: ScheduledReminder, isShort: Boolean): Spanned {
        val scheduledTime = (if (isShort)
            timeFormatter.secondsSinceEpochToConfigurableTimeString(
                scheduledReminder.timestamp.toEpochMilli() / 1000, true
            )
        else
            timeFormatter.secondsSinceEpochToConfigurableDateTimeString(
                scheduledReminder.timestamp.toEpochMilli() / 1000
            )) + ": "
        val reminderTypeSpan = getReminderTypeSpan(scheduledReminder.reminder.reminderType)

        return SpannableStringBuilder().append(reminderTypeSpan).append(scheduledTime).bold { append(scheduledReminder.medicine.medicine.name) }.append(
            getAmountOrStockString(scheduledReminder)
        )
    }

    fun getReminderTypeSpan(reminderType: ReminderEntity.ReminderType): Spanned {
        val span = SpannableStringBuilder()
        val drawable = ContextCompat.getDrawable(context, reminderType.icon)

        if (drawable != null) {
            val imageSpan = TintedImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

            span.append("  ")
            span.setSpan(imageSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        return span
    }

    private fun getAmountOrStockString(scheduledReminder: ScheduledReminder): String {
        val amount =
            when (scheduledReminder.reminder.reminderType) {
                ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                    MedicineHelper.formatAmount(scheduledReminder.medicine.medicine.amount, scheduledReminder.medicine.medicine.unit)
                }

                ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                    timeFormatter.daysSinceEpochToDateString(scheduledReminder.medicine.medicine.expirationDate)
                }

                else -> {
                    scheduledReminder.reminder.amount
                }
            }
        return if (amount.isNotEmpty()) " (${amount})" else ""
    }

    private fun getLastIntervalTime(reminderEvent: ReminderEventEntity): String =
        if (reminderEvent.lastIntervalReminderTimeInMinutes > 0 && reminderEvent.status == ReminderEventEntity.ReminderStatus.TAKEN && calcLastIntervalTime(
                reminderEvent
            ) >= 0
        ) {
            " (" + context.getString(
                R.string.interval_time, formatDuration(
                    calcLastIntervalTime(reminderEvent)
                ).toString()
            ) + ")"
        } else {
            ""
        }

    private fun calcLastIntervalTime(reminderEvent: ReminderEventEntity): Long =
        reminderEvent.processedTimestamp * 1000L - reminderEvent.lastIntervalReminderTimeInMinutes * 60_000L

    private fun getReminderTypeSpan(reminderEvent: ReminderEvent): Spanned =
        getReminderTypeSpan(reminderEvent.reminderType.toEntityReminderType())

    private fun getLastIntervalTime(reminderEvent: ReminderEvent): String {
        val lastIntervalTime = reminderEvent.lastIntervalReminderTimeInMinutes
        val processedTimestamp = reminderEvent.processedTimestamp.epochSecond
        val durationMillis = processedTimestamp * 1000L - lastIntervalTime * 60_000L
        return if (lastIntervalTime > 0 && reminderEvent.status == ReminderEvent.ReminderStatus.TAKEN && durationMillis >= 0) {
            " (" + context.getString(R.string.interval_time, formatDuration(durationMillis).toString()) + ")"
        } else {
            ""
        }
    }

    private fun statusToString(status: ReminderEvent.ReminderStatus?): String {
        return when (status) {
            ReminderEvent.ReminderStatus.TAKEN -> context.getString(R.string.taken)
            ReminderEvent.ReminderStatus.SKIPPED -> context.getString(R.string.skipped)
            else -> ""
        }
    }

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
