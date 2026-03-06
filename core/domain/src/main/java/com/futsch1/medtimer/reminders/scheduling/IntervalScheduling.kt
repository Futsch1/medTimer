package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import kotlin.math.ceil

open class IntervalScheduling(
    reminder: Reminder,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {

    override fun getNextScheduledTime(): Instant? {
        return adjustToPeriod(adjustToToday(getNextScheduledTimeInternal()))
    }

    private fun getNextScheduledTimeInternal(): Instant? {
        val lastReminderEvent: ReminderEvent =
            findLastReminderEvent() ?: return Instant.ofEpochSecond(reminder.intervalStart)

        return getNextIntervalTimeFromReminderEvent(lastReminderEvent)
    }

    private fun getNextIntervalTimeFromReminderEvent(lastReminderEvent: ReminderEvent): Instant? {

        var instant: Instant
        if (!reminder.intervalStartsFromProcessed) {
            instant = Instant.ofEpochSecond(
                lastReminderEvent.remindedTimestamp
            )
        } else if (lastReminderEvent.processedTimestamp != 0L) {
            instant = Instant.ofEpochSecond(lastReminderEvent.processedTimestamp)
        } else {
            return null
        }

        return instant.plusSeconds(reminder.timeInMinutes * 60L)
    }

    protected fun adjustToToday(instant: Instant?): Instant? {
        if (instant == null) {
            return null
        }

        var adjustedInstant = instant
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
        return adjustedInstant
    }

    protected fun adjustToPeriod(instant: Instant?): Instant? {
        if (instant == null) {
            return null
        }

        val instantDay = instant.atZone(timeAccess.systemZone()).toLocalDate().toEpochDay()
        if (reminder.periodEnd != 0L && instantDay > reminder.periodEnd) {
            return null
        }

        var adjustedInstant = instant
        if (reminder.periodStart != 0L && instantDay < reminder.periodStart) {
            val instantTimeOfDay = instant.atZone(timeAccess.systemZone()).toLocalTime()
            val periodStartLocalDateTime =
                LocalDate.ofEpochDay(reminder.periodStart).atTime(instantTimeOfDay)

            adjustedInstant = periodStartLocalDateTime.toInstant(
                timeAccess.systemZone().rules.getOffset(periodStartLocalDateTime)
            )
        }

        return adjustedInstant
    }
}
