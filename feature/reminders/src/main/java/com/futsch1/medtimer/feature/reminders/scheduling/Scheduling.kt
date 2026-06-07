package com.futsch1.medtimer.feature.reminders.scheduling

import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun interface Scheduling {
    fun getNextScheduledTime(): Instant?
}

abstract class SchedulingBase(
    protected val reminder: Reminder,
    protected val reminderEvents: List<ReminderEvent>,
    protected val timeAccess: TimeAccess
) : Scheduling {
    protected val systemZone = timeAccess.systemZone()
    protected val localDate = timeAccess.localDate()
    protected val filteredReminderEvents = filterEvents(reminderEvents)

    abstract override fun getNextScheduledTime(): Instant?

    protected fun findLastReminderEvent(): ReminderEvent? {
        return findLastReminderEvent(filteredReminderEvents, reminder.id)
    }

    protected fun findLastReminderEvent(linkedReminderId: Int): ReminderEvent? {
        return findLastReminderEvent(reminderEvents, linkedReminderId)
    }

    protected fun findLastReminderEvent(
        reminderEvents: List<ReminderEvent>,
        reminderId: Int
    ): ReminderEvent? {
        var foundReminderEvent: ReminderEvent? = null
        for (reminderEvent in reminderEvents) {
            if ((reminderEvent.reminderId == reminderId) && (foundReminderEvent == null || reminderEvent.remindedTimestamp > foundReminderEvent.remindedTimestamp)
            ) {
                foundReminderEvent = reminderEvent
            }
        }
        return foundReminderEvent
    }


    protected fun isRaisedOn(epochDay: Long): Boolean {
        for (reminderEvent in filteredReminderEvents) {
            if (isOnDay(reminderEvent.remindedTimestamp.epochSecond, epochDay)) {
                return true
            }
        }
        return false
    }

    protected fun isOnDay(epochSeconds: Long, epochDay: Long): Boolean {
        val dayInterval = getDaySecondsStartEnd(epochDay, systemZone)
        return dayInterval.first <= epochSeconds && epochSeconds < dayInterval.second
    }

    protected fun today(): Long {
        return localDate.toEpochDay()
    }

    protected fun localDateToReminderInstant(localDate: LocalDate): Instant {
        return localDate.atTime(reminder.time.getLocalTime()).atZone(
            systemZone
        ).toInstant()
    }

    protected fun getNextNotRemindedDay(start: Long = 0): Instant? {
        for (day in start..31) {
            val day = today() + day
            if (!isRaisedOn(day)) {
                return localDateToReminderInstant(LocalDate.ofEpochDay(day))
            }
        }
        return null
    }

    private fun filterEvents(reminderEvents: List<ReminderEvent>): List<ReminderEvent> {
        return reminderEvents.filter { it.reminderId == reminder.id }
    }

    companion object {
        val daySecondsStartEndCache: MutableMap<Long, Pair<Long, Long>> = mutableMapOf()

        fun getDaySecondsStartEnd(epochDay: Long, zoneId: ZoneId): Pair<Long, Long> {
            return daySecondsStartEndCache.getOrPut(epochDay) {
                val start = LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant()
                val end = LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zoneId).toInstant()
                Pair(start.epochSecond, end.epochSecond)
            }
        }
    }
}

