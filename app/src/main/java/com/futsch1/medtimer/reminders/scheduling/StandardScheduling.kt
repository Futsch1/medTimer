package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.util.Arrays
import java.util.BitSet
import kotlin.math.abs

class StandardScheduling(
    reminder: Reminder,
    reminderEventList: List<ReminderEvent>,
    timeAccess: TimeAccess
) : SchedulingBase(reminder, reminderEventList, timeAccess) {
    private val raisedToday: Boolean = isRaisedOn(today())
    private val raisedTomorrow: Boolean = isRaisedOn(today() + 1)

    // Bit map of possible days in the future on where the reminder may be raised
    private val possibleDays: BooleanArray = BooleanArray(31)

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

    private val isCyclic: Boolean
        get() = reminder.pauseDays != 0

    private fun setPossibleDaysByCycle() {
        val cycleStartDay = reminder.cycleStartDay
        var dayInCycle = today() - cycleStartDay
        val cycleLength = reminder.consecutiveDays + reminder.pauseDays
        for (x in possibleDays.indices) {
            possibleDays[x] =
                abs((dayInCycle % cycleLength).toDouble()) < reminder.consecutiveDays && dayInCycle >= 0
            dayInCycle++
        }
        // Only schedule today if it's not already raised
        possibleDays[0] = possibleDays[0] and !raisedToday
        // Only schedule tomorrow if it's not already raised
        possibleDays[1] = possibleDays[1] and !raisedTomorrow
    }

    private fun canScheduleEveryDay() {
        Arrays.fill(possibleDays, true)
        possibleDays[0] = reminderBeforeCreation() && !raisedToday
        possibleDays[1] = !raisedTomorrow
    }

    private fun clearPossibleDaysByWeekday() {
        var dayOfWeek = localDate.dayOfWeek
        for (i in possibleDays.indices) {
            if (java.lang.Boolean.FALSE == reminder.days[dayOfWeek.value - 1]) {
                possibleDays[i] = false
            }
            dayOfWeek = dayOfWeek.plus(1)
        }
    }

    private fun clearPossibleDaysByActivePeriod() {
        val today = localDate.toEpochDay()
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
        var startDate = localDate
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
                    return localDate.plusDays(i.toLong())
                }
            }
            return null
        }

    private fun reminderBeforeCreation(): Boolean {
        return reminder.createdTimestamp < localDateToReminderInstant(localDate).epochSecond
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
