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
        if ((lastReminderEvent == null
                    || lastSourceReminderEvent.processedTimestamp > lastReminderEvent.remindedTimestamp)
        ) {
            return Instant.ofEpochSecond(lastSourceReminderEvent.processedTimestamp).plusSeconds(
                reminder.timeInMinutes * 60L
            )
        }
        return null
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
