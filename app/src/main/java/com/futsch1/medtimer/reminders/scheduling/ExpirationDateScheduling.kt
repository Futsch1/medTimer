package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate

class ExpirationDateScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        if (medicine.expirationDate != 0L) {
            val startRemindDay = medicine.expirationDate - reminder.periodStart

            if (reminder.expirationReminderType == Reminder.ExpirationReminderType.ONCE) {
                if (!isRaisedOn(startRemindDay)) {
                    return localDateToReminderInstant(LocalDate.ofEpochDay(startRemindDay))
                }
            } else {
                return getNextNotRemindedDay(startRemindDay - today())
            }
        }
        return null
    }
}