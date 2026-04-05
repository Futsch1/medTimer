package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WindowedIntervalScheduling(
    reminder: ReminderEntity,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : IntervalScheduling(reminder, reminderEventList, timeAccess) {
    override fun getNextScheduledTime(): Instant? {
        return adjustToPeriod(getNextScheduledTimeInternal())
    }

    class Interval(reminder: ReminderEntity, private val date: LocalDate, private val timeAccess: TimeAccess) {
        val startInstant: Instant
        val endInstant: Instant

        init {
            startInstant = calcInstant(reminder.intervalStartTimeOfDay)
            if (reminder.intervalStartTimeOfDay <= reminder.intervalEndTimeOfDay) {
                endInstant = calcInstant(reminder.intervalEndTimeOfDay)
            } else {
                endInstant = calcInstant(reminder.intervalEndTimeOfDay).plus(1, ChronoUnit.DAYS)
            }
        }

        fun isInInterval(instant: Instant): Boolean {
            return instant in startInstant..endInstant
        }

        private fun calcInstant(minutesOfDay: Int): Instant {
            val dateTime = date.atTime(minutesOfDay / 60, minutesOfDay % 60)
            return dateTime.toInstant(timeAccess.systemZone().rules.getOffset(dateTime))
        }
    }

    fun getNextScheduledTimeInternal(): Instant? {
        val lastReminderEvent: ReminderEvent? =
            findLastReminderEvent()
        return if (lastReminderEvent != null) {
            getNextIntervalTimeFromReminderEvent(lastReminderEvent)
        } else {
            getStartInstant(timeAccess.localDate())
        }
    }

    private fun getStartInstant(date: LocalDate): Instant {
        val interval = Interval(reminder, date, timeAccess)
        val createdInstant = Instant.ofEpochSecond(reminder.createdTimestamp)
        return if (interval.startInstant.isAfter(createdInstant)) {
            interval.startInstant
        } else {
            interval.startInstant.plus(1, ChronoUnit.DAYS)
        }
    }

    private fun getNextIntervalTimeFromReminderEvent(lastReminderEvent: ReminderEvent): Instant? {
        val lastRemindedInstant = lastReminderEvent.remindedTimestamp

        val instant =
            if (reminder.intervalStartsFromProcessed) {
                if (lastReminderEvent.processedTimestamp != Instant.EPOCH)
                    lastReminderEvent.processedTimestamp
                else null
            } else
                lastRemindedInstant
        val nextTime = instant?.plusSeconds(reminder.timeInMinutes * 60L)
        // If the next interval is after the end time of this reminder's end time, go to the start of the next day
        return if (nextTime != null) {
            val interval = Interval(reminder, timeAccess.localDate(), timeAccess)
            val previousInterval = Interval(reminder, timeAccess.localDate().minusDays(1), timeAccess)
            // Check if we had a long pause, in this case, restart from today
            if (instant.isBefore(interval.startInstant.minus(1, ChronoUnit.DAYS))) {
                interval.startInstant
            } else {
                // If the last reminder is still in the previous interval, we are good
                when {
                    previousInterval.isInInterval(nextTime) -> {
                        nextTime
                    }

                    nextTime < interval.startInstant || previousInterval.isInInterval(lastRemindedInstant) && interval.isInInterval(nextTime) -> {
                        interval.startInstant
                    }

                    interval.isInInterval(nextTime) -> {
                        nextTime
                    }

                    else -> {
                        interval.startInstant.plus(1, ChronoUnit.DAYS)
                    }
                }
            }
        } else {
            nextTime
        }
    }
}
