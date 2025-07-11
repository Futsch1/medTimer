package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SchedulingItem(val medicine: FullMedicine, val reminder: Reminder)

class SchedulingSimulator(medicines: List<FullMedicine>, recentReminders: List<ReminderEvent>, startDay: LocalDate) {
    var totalEvents = mutableListOf(*recentReminders.toTypedArray())
    var schedulingItems = medicines.map { it.reminders.map { reminder -> SchedulingItem(it, reminder) } }.flatten().filter { it.reminder.active }
    val schedulingFactory = SchedulingFactory()
    var currentDay: LocalDate = startDay
    val timeAccess = object : ReminderScheduler.TimeAccess {
        override fun systemZone(): ZoneId {
            return ZoneId.systemDefault()
        }

        override fun localDate(): LocalDate {
            return currentDay
        }
    }

    fun simulate(scheduledReminderConsumer: (ScheduledReminder) -> Boolean) {
        while (simulateDay(scheduledReminderConsumer)) {
            currentDay = currentDay.plusDays(1)
        }
    }

    private fun simulateDay(scheduledReminderConsumer: (ScheduledReminder) -> Boolean): Boolean {
        var continueSimulating = true
        for (schedulingItem in schedulingItems) {
            val scheduler = schedulingFactory.create(schedulingItem.reminder, totalEvents, timeAccess)
            val nextScheduledTime = scheduler.getNextScheduledTime()
            if (nextScheduledTime != null) {
                continueSimulating = scheduledReminderConsumer(ScheduledReminder(schedulingItem.medicine, schedulingItem.reminder, nextScheduledTime))
                totalEvents.add(createReminderEvent(schedulingItem.reminder, nextScheduledTime))
            } else {
                continueSimulating = false
            }
            if (!continueSimulating) {
                break
            }
        }
        return continueSimulating
    }

    private fun createReminderEvent(
        reminder: Reminder,
        nextScheduledTime: Instant
    ): ReminderEvent {
        val reminderEvent = ReminderEvent()
        reminderEvent.remindedTimestamp = nextScheduledTime.toEpochMilli() / 1000
        reminderEvent.processedTimestamp = reminderEvent.remindedTimestamp
        reminderEvent.reminderId = reminder.reminderId
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
        return reminderEvent
    }

}