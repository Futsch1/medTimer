package com.futsch1.medtimer.reminders.scheduling

import android.content.SharedPreferences
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SchedulingFactory {
    fun create(
        reminder: Reminder,
        medicine: Medicine,
        reminderEvents: List<ReminderEvent>,
        timeAccess: ReminderScheduler.TimeAccess,
        sharedPreferences: SharedPreferences
    ): Scheduling {
        val scheduler = when (reminder.reminderType) {
            Reminder.ReminderType.LINKED -> {
                LinkedScheduling(reminder, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.CONTINUOUS_INTERVAL -> {
                IntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.WINDOWED_INTERVAL -> {
                WindowedIntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.TIME_BASED -> {
                StandardScheduling(reminder, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.OUT_OF_STOCK -> {
                OutOfStockScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.EXPIRATION_DATE -> {
                ExpirationDateScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            Reminder.ReminderType.REFILL -> {
                null
            }
        }
        return WeekendModeSchedulingDecorator(scheduler!!, timeAccess, sharedPreferences)
    }
}