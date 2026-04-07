package com.futsch1.medtimer.medicine

import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkedReminderAlgorithms @Inject constructor() {
    fun sortRemindersList(reminders: List<Reminder>): List<Reminder> {
        return reminders.sortedBy { r -> getTotalTimeInSeconds(r, reminders) }
    }

    private fun getTotalTimeInSeconds(reminder: Reminder, reminders: List<Reminder>): Long {
        var total =
            (if (reminder.reminderType != ReminderType.WINDOWED_INTERVAL) reminder.time.seconds else reminder.intervalStartTimeOfDay.toSecondOfDay().toLong())
        for (r in reminders) {
            if (r.id != reminder.linkedReminderId) {
                continue
            }

            total += when (r.reminderType) {
                ReminderType.LINKED -> {
                    getTotalTimeInSeconds(r, reminders)
                }

                else -> {
                    r.time.seconds
                }
            }
        }
        return total
    }

}