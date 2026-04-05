package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
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
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent()
        return if (lastReminderEvent != null) {
            getNextIntervalTimeFromReminderEvent(lastReminderEvent)
        } else {
            reminder.intervalStart
        }
    }

    private fun getNextIntervalTimeFromReminderEvent(lastReminderEvent: ReminderEvent): Instant? {
        val instant =
            if (reminder.intervalStartsFromProcessed) {
                if (lastReminderEvent.processedTimestamp != Instant.EPOCH)
                    lastReminderEvent.processedTimestamp
                else null
            } else
                lastReminderEvent.remindedTimestamp
        return instant?.plusSeconds(reminder.time.toSecondOfDay().toLong())
    }

    protected fun adjustToToday(instant: Instant?): Instant? {
        var adjustedInstant = instant
        if (instant != null) {
            // If the interval has been missed several times, do not re-raise the interval for all the past,
            // limit to the first interval today.
            val today = timeAccess.localDate().atStartOfDay()
            val todayInstant = today.toInstant(timeAccess.systemZone().rules.getOffset(today))
            if (instant.isBefore(todayInstant)) {
                // First interval that is triggered today
                val deltaMinutes: Long = (todayInstant.epochSecond - instant.epochSecond) / 60L
                val numIntervals = ceil(deltaMinutes.toDouble() / (reminder.time.toSecondOfDay() / 60)).toLong()
                adjustedInstant = instant.plusSeconds(numIntervals * reminder.time.toSecondOfDay())
            }
        }
        return adjustedInstant
    }

    protected fun adjustToPeriod(instant: Instant?): Instant? {
        var adjustedInstant = instant
        if (instant != null) {
            val instantDay = instant.atZone(timeAccess.systemZone()).toLocalDate()
            if (reminder.periodStart != LocalDate.EPOCH && instantDay < reminder.periodStart) {
                val instantTimeOfDay = instant.atZone(timeAccess.systemZone()).toLocalTime()
                val periodStartLocalDateTime = reminder.periodStart.atTime(instantTimeOfDay)
                adjustedInstant = periodStartLocalDateTime.toInstant(timeAccess.systemZone().rules.getOffset(periodStartLocalDateTime))
            }

            if (reminder.periodEnd != LocalDate.EPOCH && instantDay > reminder.periodEnd) {
                adjustedInstant = null
            }
        }

        return adjustedInstant
    }
}
