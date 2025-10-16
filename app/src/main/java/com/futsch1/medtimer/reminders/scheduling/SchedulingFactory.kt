package com.futsch1.medtimer.reminders.scheduling

import android.content.SharedPreferences
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SchedulingFactory {
    fun create(
        reminder: Reminder,
        filteredEvents: List<ReminderEvent>,
        timeAccess: ReminderScheduler.TimeAccess,
        sharedPreferences: SharedPreferences
    ): Scheduling {
        val scheduler = when (reminder.reminderType) {
            Reminder.ReminderType.LINKED -> {
                LinkedScheduling(reminder, filteredEvents)
            }

            Reminder.ReminderType.CONTINUOUS_INTERVAL -> {
                IntervalScheduling(reminder, filteredEvents, timeAccess)
            }

            Reminder.ReminderType.WINDOWED_INTERVAL -> {
                WindowedIntervalScheduling(reminder, filteredEvents, timeAccess)
            }

            else -> {
                StandardScheduling(reminder, filteredEvents, timeAccess)
            }
        }
        return WeekendModeSchedulingDecorator(scheduler, timeAccess, sharedPreferences)
    }
}