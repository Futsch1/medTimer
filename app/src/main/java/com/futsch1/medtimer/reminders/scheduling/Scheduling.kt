package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.stream.Collectors

fun interface Scheduling {
    fun getNextScheduledTime(): Instant?
}

abstract class SchedulingBase(
    val reminder: Reminder,
    val reminderEvents: List<ReminderEvent>,
    val timeAccess: TimeAccess
) : Scheduling {
    protected val systemZone = timeAccess.systemZone()
    protected val localDate = timeAccess.localDate()
    protected val filteredReminderEvents = filterEvents(reminderEvents)

    abstract override fun getNextScheduledTime(): Instant?

    protected fun findLastReminderEvent(): ReminderEvent? {
        return findLastReminderEvent(filteredReminderEvents, reminder.reminderId)
    }

    protected fun findLastReminderEvent(linkedReminderId: Int): ReminderEvent? {
        return findLastReminderEvent(reminderEvents, linkedReminderId)
    }

    protected fun findLastReminderEvent(reminderEvents: List<ReminderEvent>, reminderId: Int): ReminderEvent? {
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
            if (isOnDay(reminderEvent.remindedTimestamp, epochDay)) {
                return true
            }
        }
        return false
    }

    protected fun isOnDay(epochSeconds: Long, epochDay: Long): Boolean {
        return TimeHelper.secondsSinceEpochToLocalDate(epochSeconds, systemZone)
            .toEpochDay() == epochDay
    }

    protected fun today(): Long {
        return localDate.toEpochDay()
    }

    protected fun localDateToReminderInstant(localDate: LocalDate): Instant {
        return localDate.atTime(LocalTime.ofSecondOfDay(reminder.timeInMinutes * 60L)).atZone(
            systemZone
        ).toInstant()
    }

    private fun filterEvents(
        reminderEvents: List<ReminderEvent>
    ): List<ReminderEvent> {
        return reminderEvents.stream()
            .filter { event: ReminderEvent -> event.reminderId == reminder.reminderId }.collect(
                Collectors.toList()
            )
    }
}

