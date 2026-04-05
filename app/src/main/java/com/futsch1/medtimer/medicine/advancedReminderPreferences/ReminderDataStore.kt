package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.ModelDataStore
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.Reminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ReminderDataStore @AssistedInject constructor(
    @Assisted override var modelData: Reminder,
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) : ModelDataStore<Reminder>() {

    @AssistedFactory
    interface Factory {
        fun create(reminder: Reminder): ReminderDataStore
    }

    override val modelDataId: Int get() = modelData.id

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "automatically_taken" -> modelData.automaticallyTaken
            "variable_amount" -> modelData.variableAmount
            "reminder_active" -> modelData.active
            "period_start_switch" -> modelData.periodStart != LocalDate.EPOCH
            "period_end_switch" -> modelData.periodEnd != LocalDate.EPOCH
            "daily_interval" -> modelData.windowedInterval
            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            "automatically_taken" -> modelData = modelData.copy(automaticallyTaken = value)
            "variable_amount" -> modelData = modelData.copy(variableAmount = value)
            "reminder_active" -> modelData = modelData.copy(active = value)
            "period_start_switch" -> modelData = modelData.copy(periodStart = if (value) LocalDate.now() else LocalDate.EPOCH)
            "period_end_switch" -> modelData = modelData.copy(periodEnd = if (value) LocalDate.now() else LocalDate.EPOCH)
            "daily_interval" -> modelData = modelData.copy(windowedInterval = value)
        }

        updateReminder(modelData)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "instructions" -> modelData.instructions
            "cycle_start_date" -> timeFormatter.daysSinceEpochToDateString(modelData.cycleStartDay.toEpochDay())
            "cycle_consecutive_days" -> modelData.consecutiveDays.toString()
            "cycle_pause_days" -> modelData.pauseDays.toString()
            "period_start_date" -> timeFormatter.daysSinceEpochToDateString(modelData.periodStart.toEpochDay())
            "period_end_date" -> timeFormatter.daysSinceEpochToDateString(modelData.periodEnd.toEpochDay())
            "interval_start" -> if (modelData.intervalStartsFromProcessed) "1" else "0"
            "interval_start_time" -> timeFormatter.secondsSinceEpochToDateTimeString(modelData.intervalStart.epochSecond)
            "interval_daily_start_time" -> timeFormatter.minutesToTimeString(modelData.intervalStartTimeOfDay.toSecondOfDay() / 60)
            "interval_daily_end_time" -> timeFormatter.minutesToTimeString(modelData.intervalEndTimeOfDay.toSecondOfDay() / 60)
            "stock_threshold" -> MedicineHelper.formatAmount(modelData.outOfStockThreshold, "")
            "stock_reminder" -> modelData.outOfStockReminderType.ordinal.toString()
            "expiration_reminder" -> modelData.expirationReminderType.ordinal.toString()
            "expiration_days_before" -> modelData.periodStart.toEpochDay().toString()
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "instructions" -> modelData = modelData.copy(instructions = value)
            "sample_instructions" -> modelData = modelData.copy(instructions = value)
            "cycle_start_date" -> modelData = modelData.copy(cycleStartDay = timeFormatter.stringToLocalDate(value!!)!!)
            "cycle_consecutive_days" -> value?.toIntOrNull()?.let { modelData = modelData.copy(consecutiveDays = it) }
            "cycle_pause_days" -> value?.toIntOrNull()?.let { modelData = modelData.copy(pauseDays = it) }
            "period_start_date" -> modelData = modelData.copy(periodStart = timeFormatter.stringToLocalDate(value!!)!!)
            "period_end_date" -> modelData = modelData.copy(periodEnd = timeFormatter.stringToLocalDate(value!!)!!)
            "interval_start" -> modelData = modelData.copy(intervalStartsFromProcessed = value == "1")
            "interval_start_time" -> modelData = modelData.copy(intervalStart = Instant.ofEpochSecond(timeFormatter.stringToSecondsSinceEpoch(value!!)))
            "interval_daily_start_time" -> modelData = modelData.copy(intervalStartTimeOfDay = LocalTime.ofSecondOfDay(timeFormatter.timeStringToMinutes(value!!) * 60L))
            "interval_daily_end_time" -> modelData = modelData.copy(intervalEndTimeOfDay = LocalTime.ofSecondOfDay(timeFormatter.timeStringToMinutes(value!!) * 60L))
            "stock_threshold" -> MedicineHelper.parseAmount(value)?.let { modelData = modelData.copy(outOfStockThreshold = it) }
            "stock_reminder" -> modelData = modelData.copy(outOfStockReminderType = Reminder.OutOfStockReminderType.entries[value!!.toInt()])
            "expiration_reminder" -> modelData = modelData.copy(expirationReminderType = Reminder.ExpirationReminderType.entries[value!!.toInt()])
            "expiration_days_before" -> value?.toLongOrNull()?.let { modelData = modelData.copy(periodStart = LocalDate.ofEpochDay(it)) }
        }

        updateReminder(modelData)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "interval" -> modelData.time.toSecondOfDay() / 60
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "interval" -> modelData = modelData.copy(time = LocalTime.ofSecondOfDay(value * 60L))
        }

        updateReminder(modelData)
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        return when (key) {
            "remind_on_weekdays" -> {
                val dayStrings = context.resources.getStringArray(R.array.one_to_seven)
                DayOfWeek.entries.mapIndexedNotNull { i, dow ->
                    if (dow in modelData.days) dayStrings[i] else null
                }.toSet()
            }

            "remind_on_days" -> {
                val dayStrings = context.resources.getStringArray(R.array.days_of_month)
                if (modelData.activeDaysOfMonth.size < 31) {
                    dayStrings.indices
                        .filter { i -> (i + 1) in modelData.activeDaysOfMonth }
                        .map { i -> dayStrings[i] }
                        .toSet()
                } else {
                    emptySet()
                }
            }

            else -> defValues
        }
    }

    override fun putStringSet(key: String?, values: Set<String?>?) {
        when (key) {
            "remind_on_weekdays" -> {
                val dayStrings = context.resources.getStringArray(R.array.one_to_seven)
                val allDays = values?.isEmpty() == true
                modelData = modelData.copy(
                    days = DayOfWeek.entries.filterIndexed { i, _ ->
                        allDays || values?.contains(dayStrings[i]) == true
                    }
                )
            }

            "remind_on_days" -> {
                val dayStrings = context.resources.getStringArray(R.array.days_of_month)
                modelData = modelData.copy(
                    activeDaysOfMonth = if (values?.isEmpty() == true) {
                        (1..31).toList()
                    } else {
                        dayStrings.indices
                            .filter { i -> values?.contains(dayStrings[i]) == true }
                            .map { i -> i + 1 }
                    }
                )
            }
        }

        updateReminder(modelData)
    }

    private fun updateReminder(reminder: Reminder) {
        coroutineScope.launch {
            reminderRepository.update(reminder)
        }
    }
}
