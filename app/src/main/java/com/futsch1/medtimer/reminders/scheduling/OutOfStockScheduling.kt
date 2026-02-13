package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class OutOfStockScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        if (reminder.outOfStockReminderType == Reminder.OutOfStockReminderType.DAILY && medicine.amount <= reminder.outOfStockThreshold) {
            return getNextNotRemindedDay()
        }
        return null
    }
}