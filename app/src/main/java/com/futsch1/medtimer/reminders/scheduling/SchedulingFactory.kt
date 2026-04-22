package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderType
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess

class SchedulingFactory {
    fun create(
        reminder: Reminder,
        medicine: Medicine,
        reminderEvents: List<ReminderEvent>,
        timeAccess: TimeAccess,
        dataSource: PreferencesDataSource
    ): Scheduling {
        val scheduler = when (reminder.reminderType) {
            ReminderType.LINKED -> {
                LinkedScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderType.CONTINUOUS_INTERVAL -> {
                IntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderType.WINDOWED_INTERVAL -> {
                WindowedIntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderType.TIME_BASED -> {
                StandardScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderType.OUT_OF_STOCK -> {
                OutOfStockScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            ReminderType.EXPIRATION_DATE -> {
                ExpirationDateScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            ReminderType.REFILL -> {
                error("Refill reminder cannot be scheduled.")
            }
        }
        return WeekendModeSchedulingDecorator(scheduler, timeAccess, dataSource)
    }
}