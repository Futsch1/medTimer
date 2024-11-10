package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant

class LinkedScheduling(
    reminder: Reminder,
    filteredEvents: List<ReminderEvent>,
    timeAccess: ReminderScheduler.TimeAccess
) : Scheduling {
    override fun getNextScheduledTime(): Instant? {
        TODO("Not yet implemented")
    }

}
