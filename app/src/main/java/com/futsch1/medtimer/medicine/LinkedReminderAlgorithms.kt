package com.futsch1.medtimer.medicine

import com.futsch1.medtimer.database.ReminderEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkedReminderAlgorithms @Inject constructor() {
    fun sortRemindersList(reminders: List<ReminderEntity>): List<ReminderEntity> {
        return reminders.sortedBy { r -> getTotalTimeInMinutes(r, reminders) }
    }

    private fun getTotalTimeInMinutes(reminder: ReminderEntity, reminders: List<ReminderEntity>): Int {
        var total = if (reminder.reminderType != ReminderEntity.ReminderType.WINDOWED_INTERVAL) reminder.timeInMinutes else reminder.intervalStartTimeOfDay
        for (r in reminders) {
            if (r.reminderId != reminder.linkedReminderId) {
                continue
            }

            total += when (r.reminderType) {
                ReminderEntity.ReminderType.LINKED -> {
                    getTotalTimeInMinutes(r, reminders)
                }

                else -> {
                    r.timeInMinutes
                }
            }
        }
        return total
    }

}