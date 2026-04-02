package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess

class SchedulingFactory {
    fun create(
        reminder: ReminderEntity,
        medicine: MedicineEntity,
        reminderEvents: List<ReminderEventEntity>,
        timeAccess: TimeAccess,
        dataSource: PreferencesDataSource
    ): Scheduling {
        val scheduler = when (reminder.reminderType) {
            ReminderEntity.ReminderType.LINKED -> {
                LinkedScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.CONTINUOUS_INTERVAL -> {
                IntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.WINDOWED_INTERVAL -> {
                WindowedIntervalScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.TIME_BASED -> {
                StandardScheduling(reminder, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                OutOfStockScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                ExpirationDateScheduling(reminder, medicine, reminderEvents, timeAccess)
            }

            ReminderEntity.ReminderType.REFILL -> {
                error("Refill reminder cannot be scheduled.")
            }
        }
        return WeekendModeSchedulingDecorator(scheduler, timeAccess, dataSource)
    }
}