package com.futsch1.medtimer.medicine.advancedSettings

import android.content.Context
import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper

class ReminderDataStore(
    val reminderId: Int, val context: Context, val medicineRepository: MedicineRepository,
) : PreferenceDataStore() {
    var reminder: Reminder = medicineRepository.getReminder(reminderId)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return when (key) {
            "automatically_taken" -> reminder.automaticallyTaken
            "override_dnd" -> reminder.active
            else -> defValue
        }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        when (key) {
            "automatically_taken" -> reminder.automaticallyTaken = value
            "override_dnd" -> reminder.active = value
        }
        medicineRepository.updateReminder(reminder)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return when (key) {
            "instructions" -> reminder.instructions
            "cycle_start_date" -> TimeHelper.daysSinceEpochToDateString(context, reminder.cycleStartDay)
            "cycle_consecutive_days" -> reminder.consecutiveDays.toString()
            "cycle_pause_days" -> reminder.pauseDays.toString()
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