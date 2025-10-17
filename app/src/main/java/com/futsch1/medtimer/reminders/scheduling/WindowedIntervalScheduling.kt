package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate

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
                getStartInstant(timeAccess.localDate())
            }
        }
        val instant =
            if (lastReminderEvent != null) {
                getNextIntervalTimeFromReminderEvent(lastReminderEvent)
            } else {
                getStartInstant(timeAccess.localDate())
            }
        return adjustToToday(instant)
    }

    private fun getStartInstant(date: LocalDate, deltaDay: Long = 0): Instant {
        val nextDateTime = date.plusDays(deltaDay).atTime(reminder.intervalStartTimeOfDay / 60, reminder.intervalStartTimeOfDay % 60)
        val nextInstant = nextDateTime.toInstant(timeAccess.systemZone().rules.getOffset(nextDateTime))
        val createdInstant = Instant.ofEpochSecond(reminder.createdTimestamp)
        return if (nextInstant.isAfter(createdInstant)) {
            nextInstant
        } else {
            getStartInstant(date, deltaDay + 1)
        }
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
        // If the next interval is after the end time of this reminder's end time, go to the start of the next day
        return if (nextTime != null && nextTime.isAfter(getEndInstant(instant))) {
            getStartInstant(instant.atZone(timeAccess.systemZone()).toLocalDate(), 1)
        } else {
            nextTime
        }
    }
}
