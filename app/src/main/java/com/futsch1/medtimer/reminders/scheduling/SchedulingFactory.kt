package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SchedulingFactory {
    fun create(
        reminder: Reminder,
        filteredEvents: List<ReminderEvent>,
        timeAccess: ReminderScheduler.TimeAccess
    ): Scheduling {
        return when (reminder.reminderType) {
            Reminder.ReminderType.LINKED -> {
                LinkedScheduling(reminder, filteredEvents)
            }

            Reminder.ReminderType.INTERVAL_BASED -> {
                IntervalScheduling(reminder, filteredEvents, timeAccess)
            }

            else -> {
                StandardScheduling(reminder, filteredEvents, timeAccess)
            }
        }
    }
}