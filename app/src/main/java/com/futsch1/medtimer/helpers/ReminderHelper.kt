package com.futsch1.medtimer.helpers

import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import java.time.Instant
import java.time.LocalDate


fun isReminderActive(reminder: Reminder): Boolean {
    var active = reminder.active
    if (reminder.periodStart != LocalDate.EPOCH) {
        active = active && LocalDate.now() >= reminder.periodStart
    }
    if (reminder.periodEnd != LocalDate.EPOCH) {
        active = active && LocalDate.now() <= reminder.periodEnd
    }
    return active
}

fun getActiveReminders(medicine: Medicine): List<Reminder> {
    return medicine.reminders.filter { isReminderActive(it) }
}

suspend fun setRemindersActive(reminders: List<Reminder>, reminderRepository: ReminderRepository, active: Boolean) {
    val updated = reminders.map { reminder ->
        var r = reminder
        if (!reminder.active && active && reminder.reminderType == ReminderType.CONTINUOUS_INTERVAL) {
            // If reminder is activated again and an interval reminder, reset the interval start date to the current day in seconds since epoch
            r = r.copy(intervalStart = Instant.ofEpochSecond(TimeHelper.changeTimeStampDate(reminder.intervalStart.epochSecond, LocalDate.now())))
        }
        r.copy(active = active)
    }
    reminderRepository.updateAll(updated)
}
