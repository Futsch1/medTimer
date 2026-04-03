package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class LinkedScheduling(
    reminder: ReminderEntity,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        val lastSourceReminderEvent: ReminderEvent? =
            findLastReminderEvent(reminder.linkedReminderId)
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent()
        if (lastSourceReminderEvent != null &&
            lastSourceReminderEvent.processedTimestamp != Instant.EPOCH &&
            (lastReminderEvent == null
                    || lastSourceReminderEvent.processedTimestamp > lastReminderEvent.remindedTimestamp)
        ) {
            return lastSourceReminderEvent.processedTimestamp.plusSeconds(
                reminder.timeInMinutes * 60L
            )
        }
        return null
    }

}
