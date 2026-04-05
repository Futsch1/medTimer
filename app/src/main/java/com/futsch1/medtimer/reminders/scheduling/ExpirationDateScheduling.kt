package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
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
        val firstRemindedDay = medicine.expirationDate.toEpochDay() - reminder.periodStart.toEpochDay()

        if (medicine.expirationDate != LocalDate.EPOCH) {
            return if (reminder.expirationReminderType == Reminder.ExpirationReminderType.ONCE) {
                scheduleOnce(firstRemindedDay)
            } else {
                val startRemindDay = max(firstRemindedDay, today())
                getNextNotRemindedDay((startRemindDay - today()).coerceAtLeast(0))
            }
        }
        return null
    }

    private fun scheduleOnce(firstRemindedDay: Long): Instant? {
        return if (firstRemindedDay <= today()) {
            for (day in (medicine.expirationDate.toEpochDay() - reminder.periodStart.toEpochDay()..today()))
                if (isRaisedOn(day)) {
                    return null
                }
            localDateToReminderInstant(LocalDate.ofEpochDay(today()))
        } else {
            null
        }
    }
}