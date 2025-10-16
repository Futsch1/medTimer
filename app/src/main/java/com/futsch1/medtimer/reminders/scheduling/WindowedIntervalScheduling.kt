package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant

class WindowedIntervalScheduling(
    private val reminder: Reminder,
    private val reminderEventList: List<ReminderEvent>,
    private val timeAccess: TimeAccess
) : IntervalScheduling(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent(reminder.reminderId, reminderEventList)
        if (reminder.intervalStartsFromProcessed) {
            return if (lastReminderEvent != null) {
                getNextIntervalTimeFromReminderEvent(lastReminderEvent)
            } else {
                getStartInstant(0)
            }
        }
        val instant =
            if (lastReminderEvent != null) {
                getNextIntervalTimeFromReminderEvent(lastReminderEvent)
            } else {
                getStartInstant(0)
            }
        return adjustToToday(instant)
    }

    private fun getStartInstant(deltaDay: Long = 0): Instant {
        val nextDateTime = timeAccess.localDate().plusDays(deltaDay).atTime(reminder.intervalStartTimeOfDay / 60, reminder.intervalStartTimeOfDay % 60)
        return nextDateTime.toInstant(timeAccess.systemZone().rules.getOffset(nextDateTime))
    }

    private fun getEndInstant(nextTime: Instant): Instant {
        val endDateTime = nextTime.atZone(timeAccess.systemZone()).withHour(reminder.intervalEndTimeOfDay / 60).withMinute(reminder.intervalEndTimeOfDay % 60)
        return endDateTime.toInstant()
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
        val nextTime = instant?.plusSeconds(reminder.timeInMinutes * 60L)
        return if (nextTime != null && nextTime.isAfter(getEndInstant(nextTime))) {
            getStartInstant(1)
        } else {
            nextTime
        }
    }
}
