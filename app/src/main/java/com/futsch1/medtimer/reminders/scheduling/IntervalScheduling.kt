package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant

class IntervalScheduling(
    private val reminder: Reminder,
    private val reminderEventList: List<ReminderEvent>
) : Scheduling {
    override fun getNextScheduledTime(): Instant? {
        val lastReminderEvent: ReminderEvent? = findLastReminderEvent(reminder.reminderId)
        return if (lastReminderEvent != null && lastReminderEvent.remindedTimestamp >= reminder.intervalStart) {
            return getNextIntervalTimeFromReminderEvent(lastReminderEvent)
        } else {
            Instant.ofEpochSecond(reminder.intervalStart)
        }
    }

    private fun getNextIntervalTimeFromReminderEvent(lastReminderEvent: ReminderEvent): Instant? {
        val instant =
            if (reminder.intervalStartsFromProcessed) {
                if (lastReminderEvent.processedTimestamp != 0L)
                    Instant.ofEpochSecond(lastReminderEvent.processedTimestamp)
                else null
            } else
                Instant.ofEpochSecond(
                    lastReminderEvent.remindedTimestamp
                )
        return instant?.plusSeconds(reminder.timeInMinutes * 60L)
    }

    private fun findLastReminderEvent(reminderId: Int): ReminderEvent? {
        var foundReminderEvent: ReminderEvent? = null
        for (reminderEvent in reminderEventList) {
            if (reminderEvent.reminderId == reminderId &&
                (foundReminderEvent == null || reminderEvent.remindedTimestamp > foundReminderEvent.remindedTimestamp)
            ) {
                foundReminderEvent = reminderEvent
            }
        }
        return foundReminderEvent
    }

}
