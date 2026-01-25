package com.futsch1.medtimer.medicine.advancedReminderPreferences

import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.EntityDataStore
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import java.time.LocalDate

class ReminderDataStore(
    override val entityId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : EntityDataStore<Reminder>() {
    override var entity: Reminder = medicineRepository.getReminder(entityId)!!

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
        medicineRepository.updateReminder(entity)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "instructions" -> entity.instructions
            "cycle_start_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.cycleStartDay)
            "cycle_consecutive_days" -> entity.consecutiveDays.toString()
            "cycle_pause_days" -> entity.pauseDays.toString()
            "period_start_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.periodStart)
            "period_end_date" -> TimeHelper.daysSinceEpochToDateString(context, entity.periodEnd)
            "interval_start" -> if (entity.intervalStartsFromProcessed) "1" else "0"
            "interval_start_time" -> TimeHelper.secondsSinceEpochToDateTimeString(context, entity.intervalStart)
            "interval_daily_start_time" -> TimeHelper.minutesToTimeString(context, entity.intervalStartTimeOfDay.toLong())
            "interval_daily_end_time" -> TimeHelper.minutesToTimeString(context, entity.intervalEndTimeOfDay.toLong())
            "stock_threshold" -> MedicineHelper.formatAmount(entity.outOfStockThreshold, "")
            "stock_reminder" -> entity.outOfStockReminderType.ordinal.toString()
            "expiration_days_before" -> entity.periodStart.toString()
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "instructions" -> entity.instructions = value!!
            "sample_instructions" -> entity.instructions = value!!
            "cycle_start_date" -> entity.cycleStartDay = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "cycle_consecutive_days" -> value?.toIntOrNull()?.let { entity.consecutiveDays = it }
            "cycle_pause_days" -> value?.toIntOrNull()?.let { entity.pauseDays = it }
            "period_start_date" -> entity.periodStart = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "period_end_date" -> entity.periodEnd = TimeHelper.stringToLocalDate(context, value!!)!!.toEpochDay()
            "interval_start" -> entity.intervalStartsFromProcessed = value == "1"
            "interval_start_time" -> entity.intervalStart = TimeHelper.stringToSecondsSinceEpoch(context, value!!)
            "interval_daily_start_time" -> entity.intervalStartTimeOfDay = TimeHelper.timeStringToMinutes(context, value!!)
            "interval_daily_end_time" -> entity.intervalEndTimeOfDay = TimeHelper.timeStringToMinutes(context, value!!)
            "stock_threshold" -> MedicineHelper.parseAmount(value)?.let { entity.outOfStockThreshold = it }
            "stock_reminder" -> entity.outOfStockReminderType = Reminder.OutOfStockReminderType.entries[value!!.toInt()]
            "expiration_days_before" -> value?.toLongOrNull()?.let { entity.periodStart = it }
        }
        medicineRepository.updateReminder(entity)
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
        medicineRepository.updateReminder(entity)
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

        medicineRepository.updateReminder(entity)
    }
}