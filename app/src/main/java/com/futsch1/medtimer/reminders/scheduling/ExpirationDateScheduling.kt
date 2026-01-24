package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant

class ExpirationDateScheduling(
    reminder: Reminder,
    val medicine: Medicine,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        TODO("Not yet implemented")
    }
}