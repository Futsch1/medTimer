package com.futsch1.medtimer.helpers

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderRepository
import java.time.LocalDate


fun isReminderActive(reminder: ReminderEntity): Boolean {
    var active = reminder.active
    if (reminder.periodStart != 0L) {
        active = active && LocalDate.now().toEpochDay() >= reminder.periodStart
    }
    if (reminder.periodEnd != 0L) {
        active = active && LocalDate.now().toEpochDay() <= reminder.periodEnd
    }
    return active
}

fun getActiveReminders(medicine: FullMedicineEntity): List<ReminderEntity> {
    return medicine.reminders.filter { isReminderActive(it) }
}

suspend fun setRemindersActive(reminders: List<ReminderEntity>, reminderRepository: ReminderRepository, active: Boolean) {
    for (reminder in reminders) {
        if (!reminder.active && active && reminder.reminderType == ReminderEntity.ReminderType.CONTINUOUS_INTERVAL) {
            // If reminder is activated again and an interval reminder, reset the interval start date to the current day in seconds since epoch
            reminder.intervalStart = TimeHelper.changeTimeStampDate(reminder.intervalStart, LocalDate.now())
        }
        reminder.active = active
    }
    reminderRepository.updateAll(reminders)
}
