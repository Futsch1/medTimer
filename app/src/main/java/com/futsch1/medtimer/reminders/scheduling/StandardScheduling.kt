package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Arrays
import java.util.BitSet
import java.util.stream.Collectors
import kotlin.math.abs

class StandardScheduling(
    private val reminder: Reminder,
    reminderEventList: List<ReminderEvent>,
    private val timeAccess: TimeAccess
) : Scheduling {
    private val raisedToday: Boolean
    private val possibleDays: BooleanArray

    init {
        this.raisedToday = isRaisedToday(filterEvents(reminderEventList))
        // Bit map of possible days in the future on where the reminder may be raised
        this.possibleDays = BooleanArray(31)
    }

    private fun isRaisedToday(reminderEventList: List<ReminderEvent>): Boolean {
        for (reminderEvent in reminderEventList) {
            if (isToday(reminderEvent.remindedTimestamp)) {
                return true
            }
        }
        return false
    }

    private fun isToday(epochSeconds: Long): Boolean {
        return localDateFromEpochSeconds(epochSeconds).toEpochDay() == today()
    }

    private fun localDateFromEpochSeconds(epochSeconds: Long): LocalDate {
        return Instant.ofEpochSecond(epochSeconds).atZone(timeAccess.systemZone()).toLocalDate()
    }

    private fun today(): Long {
        return timeAccess.localDate().toEpochDay()
    }

    private val nextScheduledDate: LocalDate?
        get() {
            if (isCyclic) {
                setPossibleDaysByCycle()
            } else {
                canScheduleEveryDay()
            }

            clearPossibleDaysByWeekday()
            clearPossibleDaysByActivePeriod()
            clearPossibleDaysByActiveDayOfMonth()

            return earliestPossibleDate
        }

    private fun localDateToReminderInstant(localDate: LocalDate): Instant {
        return localDate.atTime(LocalTime.ofSecondOfDay(reminder.timeInMinutes * 60L)).atZone(
            timeAccess.systemZone()
        ).toInstant()
    }

    private val isCyclic: Boolean
        get() = reminder.pauseDays != 0

    private fun setPossibleDaysByCycle() {
        val cycleStartDay = reminder.cycleStartDay
        var dayInCycle = today() - cycleStartDay
        val cycleLength = reminder.consecutiveDays + reminder.pauseDays
        for (x in possibleDays.indices) {
            possibleDays[x] =
                abs((dayInCycle % cycleLength).toDouble()) < reminder.consecutiveDays && dayInCycle + x >= 0
            dayInCycle++
        }
        // Only schedule today if it's not already raised
        possibleDays[0] = possibleDays[0] and !raisedToday
    }

    private fun canScheduleEveryDay() {
        Arrays.fill(possibleDays, true)
        possibleDays[0] = reminderBeforeCreation() && !raisedToday
    }

    private fun clearPossibleDaysByWeekday() {
        var dayOfWeek = timeAccess.localDate().dayOfWeek
        for (i in possibleDays.indices) {
            if (java.lang.Boolean.FALSE == reminder.days[dayOfWeek.value - 1]) {
                possibleDays[i] = false
            }
            dayOfWeek = dayOfWeek.plus(1)
        }
    }

    private fun clearPossibleDaysByActivePeriod() {
        val today = timeAccess.localDate().toEpochDay()
        for (i in possibleDays.indices) {
            if (reminder.periodStart != 0L && today + i < reminder.periodStart) {
                possibleDays[i] = false
            }
            if (reminder.periodEnd != 0L && today + i > reminder.periodEnd) {
                possibleDays[i] = false
            }
        }
    }

    private fun clearPossibleDaysByActiveDayOfMonth() {
        var startDate = timeAccess.localDate()
        val bitSet = BitSet.valueOf(longArrayOf(reminder.activeDaysOfMonth.toLong()))
        for (i in possibleDays.indices) {
            possibleDays[i] = possibleDays[i] and bitSet[startDate.dayOfMonth - 1]
            startDate = startDate.plusDays(1)
        }
    }

    private val earliestPossibleDate: LocalDate?
        get() {
            for (i in possibleDays.indices) {
                if (possibleDays[i]) {
                    return timeAccess.localDate().plusDays(i.toLong())
                }
            }
            return null
        }

    private fun reminderBeforeCreation(): Boolean {
        return reminder.createdTimestamp < localDateToReminderInstant(timeAccess.localDate()).epochSecond
    }

    private fun filterEvents(
        reminderEvents: List<ReminderEvent>
    ): List<ReminderEvent> {
        return reminderEvents.stream()
            .filter { event: ReminderEvent -> event.reminderId == reminder.reminderId }.collect(
                Collectors.toList()
            )
    }


    override fun getNextScheduledTime(): Instant? {
        val nextScheduledDate = nextScheduledDate
        return if (nextScheduledDate != null) {
            localDateToReminderInstant(nextScheduledDate)
        } else {
            null
        }
    }
}
