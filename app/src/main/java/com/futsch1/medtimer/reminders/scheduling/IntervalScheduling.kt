package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import kotlin.math.ceil

class IntervalScheduling(
    private val reminder: Reminder,
    private val reminderEventList: List<ReminderEvent>,
    private val timeAccess: TimeAccess
) : Scheduling {
    override fun getNextScheduledTime(): Instant? {
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent(reminder.reminderId, reminderEventList)
        if (reminder.intervalStartsFromProcessed) {
            return if (lastReminderEvent != null) {
                getNextIntervalTimeFromReminderEvent(lastReminderEvent)
            } else {
                Instant.ofEpochSecond(reminder.intervalStart)
            }
        }
        val instant =
            if (lastReminderEvent != null && lastReminderEvent.remindedTimestamp >= reminder.intervalStart) {
                getNextIntervalTimeFromReminderEvent(lastReminderEvent)
            } else {
                Instant.ofEpochSecond(reminder.intervalStart)
            }
        return adjustToToday(instant)
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

    private fun adjustToToday(instant: Instant?): Instant? {
        var adjustedInstant = instant
        if (instant != null) {
            // If the interval has been missed several times, do not re-raise the interval for all the past,
            // limit to the first interval today.
            val today = timeAccess.localDate().atStartOfDay()
            val todayInstant = today.toInstant(timeAccess.systemZone().rules.getOffset(today))
            if (instant.isBefore(todayInstant)) {
                // First interval that is triggered today
                val deltaMinutes: Long = (todayInstant.epochSecond - instant.epochSecond) / 60L
                val numIntervals = ceil(deltaMinutes.toDouble() / reminder.timeInMinutes).toLong()
                adjustedInstant = instant.plusSeconds(numIntervals * reminder.timeInMinutes * 60L)
            }
        }
        return adjustedInstant
    }
}
