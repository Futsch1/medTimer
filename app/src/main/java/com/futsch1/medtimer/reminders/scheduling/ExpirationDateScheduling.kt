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

        if (medicine.expirationDate != 0L) {
            return if (reminder.expirationReminderType == Reminder.ExpirationReminderType.ONCE) {
                scheduleOnce(firstRemindedDay)
            } else {
                val startRemindDay = max(medicine.expirationDate - reminder.periodStart, today())
                getNextNotRemindedDay((startRemindDay - today()).coerceAtLeast(0))
            }
        }
        return null
    }

    private fun scheduleOnce(firstRemindedDay: Long): Instant? {
        return if (firstRemindedDay <= today()) {
            for (day in (medicine.expirationDate - reminder.periodStart..today()))
                if (isRaisedOn(day)) {
                    return null
                }
            localDateToReminderInstant(LocalDate.ofEpochDay(today()))
        } else {
            null
        }
    }
}