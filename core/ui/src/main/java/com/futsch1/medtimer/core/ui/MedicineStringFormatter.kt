package com.futsch1.medtimer.core.ui

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineStringFormatter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeFormatter: TimeFormatter
) {
    fun getMedicineNameWithStockText(medicine: Medicine): SpannableStringBuilder {
        return getMedicineNameWithStockText(preferencesDataSource.preferences.value, medicine)
    }

    fun getMedicineNameWithStockText(
        userPreferences: UserPreferences,
        medicine: Medicine
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder().bold {
            append(
                MedicineHelper.getMedicineName(
                    medicine,
                    false,
                    userPreferences
                )
            )
        }
        builder.append(getStockTextWithIcons(medicine))
        return builder
    }

    private fun getStockTextWithIcons(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        val stockIconText = MedicineHelper.getStockIcons(medicine)
        val stockText = if (medicine.isStockManagementActive()) getStockText(medicine) else ""

        if (stockIconText.isNotEmpty() || stockText.isNotEmpty()) {
            builder.append(" (")
            builder.append(stockText)
            if (stockText.isNotEmpty() && stockIconText.isNotEmpty())
                builder.append(" ")
            builder.append(stockIconText)
            builder.append(")")
        }
        return builder
    }

    fun getDatesText(medicine: Medicine): SpannableStringBuilder {
        val s = SpannableStringBuilder()

        if (medicine.productionDate != LocalDate.EPOCH) {
            s.append(context.getString(R.string.production_date))
            s.append(": ")
            s.append(timeFormatter.localDateToString(medicine.productionDate))
        }
        if (medicine.expirationDate != LocalDate.EPOCH) {
            if (s.isNotEmpty()) {
                s.append(", ")
            }
            s.append(context.getString(R.string.expiration_date))
            s.append(": ")
            s.append(timeFormatter.localDateToString(medicine.expirationDate))
            val expiredIcon = MedicineHelper.getExpiredIcon(medicine)
            if (expiredIcon.isNotEmpty()) {
                s.append(" ")
                s.append(expiredIcon)
            }
        }
        return s
    }

    fun getStockText(medicine: Medicine): String {
        return context.getString(
            R.string.medicine_stock_string,
            MedicineHelper.formatAmount(medicine.amount, medicine.unit)
        )
    }

    fun getStockRunOutText(runOutDate: LocalDate?, simulatedThrough: LocalDate): String {
        return when (runOutDate) {
            null -> "---"
            LocalDate.MAX -> context.getString(
                R.string.stock_after_simulation_end,
                timeFormatter.localDateToString(simulatedThrough)
            )
            else -> timeFormatter.localDateToString(runOutDate)
        }
    }

    fun getReminderTimes(medicine: Medicine): List<String> {
        val reminders = medicine.reminders.filter { it.active }
        val reminderTimes = buildList {
            addAll(timeBasedRemindersSummary(reminders.filter { it.reminderType == ReminderType.TIME_BASED }))
            addAll(reminders.filter { it.reminderType == ReminderType.LINKED }
                .map { linkedReminderSummaryString(medicine, it) })
            addAll(reminders.filter { it.isInterval }.map { intervalBasedReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderType.OUT_OF_STOCK }
                .map { outOfStockReminderString(it) })
            addAll(reminders.filter { it.reminderType == ReminderType.EXPIRATION_DATE }
                .map { context.getString(R.string.expiration_date) })
        }

        return reminderTimes
    }

    private fun linkedReminderSummaryString(medicine: Medicine, reminder: Reminder): String {
        val delays = mutableListOf<String>()
        var current = reminder

        while (current.reminderType == ReminderType.LINKED) {
            delays.add(timeFormatter.toTimeString(current.time))
            val source =
                medicine.reminders.findLast { it.id == current.linkedReminderId } ?: return "?"
            current = source
        }

        val base = timeFormatter.toTimeString(current.time)
        return if (delays.isEmpty()) base
        else "$base + ${delays.reversed().joinToString(" + ")}"
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

    private fun timeBasedRemindersSummary(reminders: List<Reminder>): List<String> {
        return reminders.map { r -> r.time }.sorted()
            .map { timeFormatter.toTimeString(it) }
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
}
