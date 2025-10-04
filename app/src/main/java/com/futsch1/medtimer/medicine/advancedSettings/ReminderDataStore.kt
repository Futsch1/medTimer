package com.futsch1.medtimer.medicine.advancedSettings

import androidx.preference.PreferenceDataStore
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder

class ReminderDataStore(
    val reminderId: Int, val medicineRepository: MedicineRepository,
) : PreferenceDataStore() {
    var reminder: Reminder = medicineRepository.getReminder(reminderId)

    init {
        reminder
    }

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
        when (key) {
            "instructions" -> return reminder.instructions
            else -> return defValue
        }
    }

    override fun putString(key: String?, value: String?) {
        when (key) {
            "instructions" -> reminder.instructions = value!!
        }
        medicineRepository.updateReminder(reminder)
    }
}