package com.futsch1.medtimer.medicine

import com.futsch1.medtimer.database.Reminder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkedReminderAlgorithms @Inject constructor() {
    fun sortRemindersList(reminders: List<Reminder>): List<Reminder> {
        return reminders.sortedBy { r -> getTotalTimeInMinutes(r, reminders) }
    }

    private fun getTotalTimeInMinutes(reminder: Reminder, reminders: List<Reminder>): Int {
        var total = if (reminder.reminderType != Reminder.ReminderType.WINDOWED_INTERVAL) reminder.timeInMinutes else reminder.intervalStartTimeOfDay
        for (r in reminders) {
            if (r.reminderId != reminder.linkedReminderId) {
                continue
            }

            total += when (r.reminderType) {
                Reminder.ReminderType.LINKED -> {
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