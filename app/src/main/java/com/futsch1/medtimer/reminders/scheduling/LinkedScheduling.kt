package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant

class LinkedScheduling(
    private val reminder: Reminder,
    private val reminderEventList: List<ReminderEvent>
) : Scheduling {
    override fun getNextScheduledTime(): Instant? {
        val lastSourceReminderEvent: ReminderEvent? =
            findLastReminderEvent(reminder.linkedReminderId, reminderEventList)
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent(reminder.reminderId, reminderEventList)
        if (lastSourceReminderEvent != null &&
            lastSourceReminderEvent.processedTimestamp != 0L &&
            (lastReminderEvent == null
                    || lastSourceReminderEvent.processedTimestamp > lastReminderEvent.remindedTimestamp)
        ) {
            return Instant.ofEpochSecond(lastSourceReminderEvent.processedTimestamp).plusSeconds(
                reminder.timeInMinutes * 60L
            )
        }
        return null
    }
}
