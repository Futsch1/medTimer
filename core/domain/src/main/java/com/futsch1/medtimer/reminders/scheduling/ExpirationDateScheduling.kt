package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import kotlin.math.max

class ExpirationDateScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        val firstRemindedDay = medicine.expirationDate - reminder.periodStart

        if (medicine.expirationDate == 0L) {
            return null
        }

        if (reminder.expirationReminderType == Reminder.ExpirationReminderType.ONCE) {
            return scheduleOnce(firstRemindedDay)
        }

        val startRemindDay = max(medicine.expirationDate - reminder.periodStart, today())
        return getNextNotRemindedDay((startRemindDay - today()).coerceAtLeast(0))
    }

    private fun scheduleOnce(firstRemindedDay: Long): Instant? {
        if (firstRemindedDay > today()) {
            return null
        }

        for (day in (medicine.expirationDate - reminder.periodStart..today())) {
            if (isRaisedOn(day)) {
                return null
            }
        }
        return localDateToReminderInstant(LocalDate.ofEpochDay(today()))
    }
}