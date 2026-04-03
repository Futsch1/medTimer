package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderDataStore @AssistedInject constructor(
    @Assisted override var entity: Reminder,
    @param:ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val timeFormatter: TimeFormatter,
    @param:ApplicationScope private val coroutineScope: CoroutineScope
) : EntityDataStore<Reminder>() {

    @AssistedFactory
    interface Factory {
        fun create(entity: Reminder): ReminderDataStore
    }

    override val entityId: Int get() = entity.reminderId

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "automatically_taken" -> entity.automaticallyTaken
            "variable_amount" -> entity.variableAmount
            "reminder_active" -> entity.active
            "period_start_switch" -> entity.periodStart != 0L
            "period_end_switch" -> entity.periodEnd != 0L
            "daily_interval" -> entity.windowedInterval
            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            "automatically_taken" -> entity.automaticallyTaken = value
            "variable_amount" -> entity.variableAmount = value
            "reminder_active" -> entity.active = value

            "period_start_switch" -> {
                if (value) {
                    entity.periodStart = LocalDate.now().toEpochDay()
                } else {
                    entity.periodStart = 0
                }
            }

            "period_end_switch" -> {
                if (value) {
                    entity.periodEnd = LocalDate.now().toEpochDay()
                } else {
                    entity.periodEnd = 0
                }
            }

            "daily_interval" -> entity.windowedInterval = value
        }

        updateReminder(entity)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "instructions" -> entity.instructions
            "cycle_start_date" -> timeFormatter.daysSinceEpochToDateString(entity.cycleStartDay)
            "cycle_consecutive_days" -> entity.consecutiveDays.toString()
            "cycle_pause_days" -> entity.pauseDays.toString()
            "period_start_date" -> timeFormatter.daysSinceEpochToDateString(entity.periodStart)
            "period_end_date" -> timeFormatter.daysSinceEpochToDateString(entity.periodEnd)
            "interval_start" -> if (entity.intervalStartsFromProcessed) "1" else "0"
            "interval_start_time" -> timeFormatter.secondsSinceEpochToDateTimeString(entity.intervalStart)
            "interval_daily_start_time" -> timeFormatter.minutesToTimeString(entity.intervalStartTimeOfDay)
            "interval_daily_end_time" -> timeFormatter.minutesToTimeString(entity.intervalEndTimeOfDay)
            "stock_threshold" -> MedicineHelper.formatAmount(entity.outOfStockThreshold, "")
            "stock_reminder" -> entity.outOfStockReminderType.ordinal.toString()
            "expiration_reminder" -> entity.expirationReminderType.ordinal.toString()
            "expiration_days_before" -> entity.periodStart.toString()
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "instructions" -> entity.instructions = value!!
            "sample_instructions" -> entity.instructions = value!!
            "cycle_start_date" -> entity.cycleStartDay = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
            "cycle_consecutive_days" -> value?.toIntOrNull()?.let { entity.consecutiveDays = it }
            "cycle_pause_days" -> value?.toIntOrNull()?.let { entity.pauseDays = it }
            "period_start_date" -> entity.periodStart = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
            "period_end_date" -> entity.periodEnd = timeFormatter.stringToLocalDate(value!!)!!.toEpochDay()
            "interval_start" -> entity.intervalStartsFromProcessed = value == "1"
            "interval_start_time" -> entity.intervalStart = timeFormatter.stringToSecondsSinceEpoch(value!!)
            "interval_daily_start_time" -> entity.intervalStartTimeOfDay = timeFormatter.timeStringToMinutes(value!!)
            "interval_daily_end_time" -> entity.intervalEndTimeOfDay = timeFormatter.timeStringToMinutes(value!!)
            "stock_threshold" -> MedicineHelper.parseAmount(value)?.let { entity.outOfStockThreshold = it }
            "stock_reminder" -> entity.outOfStockReminderType = Reminder.OutOfStockReminderType.entries[value!!.toInt()]
            "expiration_reminder" -> entity.expirationReminderType =
                Reminder.ExpirationReminderType.entries[value!!.toInt()]

            "expiration_days_before" -> value?.toLongOrNull()?.let { entity.periodStart = it }
        }

        updateReminder(entity)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "interval" -> entity.timeInMinutes
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "interval" -> entity.timeInMinutes = value
        }

        updateReminder(entity)
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        return when (key) {
            "remind_on_weekdays" -> {
                val values: MutableSet<String> = mutableSetOf()
                val days = context.resources.getStringArray(R.array.one_to_seven)
                for (i in entity.days.indices) {
                    if (entity.days[i]) {
                        values += days[i]
                    }
                }
                values
            }

            "remind_on_days" -> {
                val values: MutableSet<String> = mutableSetOf()
                val days = context.resources.getStringArray(R.array.days_of_month)
                if ((entity.activeDaysOfMonth and 0x7FFF_FFFF) != 0x7FFF_FFFF) {
                    for (i in days.indices) {
                        if ((entity.activeDaysOfMonth and (1 shl i)) > 0) {
                            values += days[i]
                        }
                    }
                }
                values
            }

            else -> defValues
        }
    }

    override fun putStringSet(key: String?, values: Set<String?>?) {
        when (key) {
            "remind_on_weekdays" -> {
                val days = context.resources.getStringArray(R.array.one_to_seven)
                for (i in entity.days.indices) {
                    entity.days[i] = values?.contains(days[i]) == true || values?.isEmpty() == true
                }
            }

            "remind_on_days" -> {
                val days = context.resources.getStringArray(R.array.days_of_month)
                if (values?.isEmpty() == true) {
                    entity.activeDaysOfMonth = 0x7FFF_FFFF
                } else {
                    entity.activeDaysOfMonth = 0
                    for (i in days.indices) {
                        if (values?.contains(days[i]) == true) {
                            entity.activeDaysOfMonth = entity.activeDaysOfMonth or (1 shl i)
                        }
                    }
                }
            }
        }

        updateReminder(entity)
    }

    private fun updateReminder(reminder: Reminder) {
        coroutineScope.launch {
            medicineRepository.updateReminder(reminder)
        }
    }
}
