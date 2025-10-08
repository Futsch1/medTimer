package com.futsch1.medtimer.medicine.advancedSettings

import android.content.Context
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import java.time.LocalDate

class ReminderDataStore(
    val reminderId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : PreferenceDataStore() {
    var reminder: Reminder = medicineRepository.getReminder(reminderId)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "automatically_taken" -> reminder.automaticallyTaken
            "variable_amount" -> reminder.variableAmount
            "reminder_active" -> reminder.active
            "period_start_switch" -> reminder.periodStart != 0L
            "period_end_switch" -> reminder.periodEnd != 0L
            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            "automatically_taken" -> reminder.automaticallyTaken = value
            "variable_amount" -> reminder.variableAmount = value
            "reminder_active" -> reminder.active = value

            "period_start_switch" -> {
                if (value) {
                    reminder.periodStart = LocalDate.now().toEpochDay()
                } else {
                    reminder.periodStart = 0
                }
            }

            "period_end_switch" -> {
                if (value) {
                    reminder.periodEnd = LocalDate.now().toEpochDay()
                } else {
                    reminder.periodEnd = 0
                }
            }
        }
        medicineRepository.updateReminder(reminder)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "instructions" -> reminder.instructions
            "cycle_start_date" -> TimeHelper.daysSinceEpochToDateString(context, reminder.cycleStartDay)
            "cycle_consecutive_days" -> reminder.consecutiveDays.toString()
            "cycle_pause_days" -> reminder.pauseDays.toString()
            "period_start_date" -> TimeHelper.daysSinceEpochToDateString(context, reminder.periodStart)
            "period_end_date" -> TimeHelper.daysSinceEpochToDateString(context, reminder.periodEnd)
            else -> defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "instructions" -> reminder.instructions = value!!
            "cycle_start_date" -> reminder.cycleStartDay = TimeHelper.dateStringToDate(context, value!!)!!.toEpochDay()
            "cycle_consecutive_days" -> try {
                reminder.consecutiveDays = value!!.toInt()
            } catch (_: NumberFormatException) { /* Intentionally empty */
            }

            "cycle_pause_days" -> try {
                reminder.pauseDays = value!!.toInt()
            } catch (_: NumberFormatException) { /* Intentionally empty */
            }

            "period_start_date" -> reminder.periodStart = TimeHelper.dateStringToDate(context, value!!)!!.toEpochDay()
            "period_end_date" -> reminder.periodEnd = TimeHelper.dateStringToDate(context, value!!)!!.toEpochDay()
        }
        medicineRepository.updateReminder(reminder)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "interval" -> return reminder.timeInMinutes
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "interval" -> reminder.timeInMinutes = value
        }
        medicineRepository.updateReminder(reminder)
    }
}