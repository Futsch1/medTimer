package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class LinkedScheduling(
    reminder: Reminder,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        val lastSourceReminderEvent: ReminderEvent =
            findLastReminderEvent(reminder.linkedReminderId) ?: return null

        if (lastSourceReminderEvent.processedTimestamp == 0L) {
            return null
        }

        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent()
        if (lastReminderEvent != null && lastSourceReminderEvent.processedTimestamp <= lastReminderEvent.remindedTimestamp) {
            return null
        }

        return Instant.ofEpochSecond(lastSourceReminderEvent.processedTimestamp).plusSeconds(
            reminder.timeInMinutes * 60L
        )
    }
}
