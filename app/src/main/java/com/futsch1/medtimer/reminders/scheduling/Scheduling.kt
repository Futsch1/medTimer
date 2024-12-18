package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant

fun interface Scheduling {
    fun getNextScheduledTime(): Instant?
}

fun findLastReminderEvent(reminderId: Int, reminderEventList: List<ReminderEvent>): ReminderEvent? {
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