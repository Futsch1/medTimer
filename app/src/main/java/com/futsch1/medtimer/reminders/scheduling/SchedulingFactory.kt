package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SchedulingFactory {
    fun create(
        reminder: Reminder,
        filteredEvents: List<ReminderEvent>,
        timeAccess: ReminderScheduler.TimeAccess
    ): Scheduling {
        return if (reminder.linkedReminderId == 0) {
            StandardScheduling(reminder, filteredEvents, timeAccess)
        } else {
            LinkedScheduling(reminder, filteredEvents, timeAccess)
        }
    }
}