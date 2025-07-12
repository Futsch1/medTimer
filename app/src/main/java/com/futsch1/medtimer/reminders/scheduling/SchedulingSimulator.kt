package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.MedicineHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SchedulingItem(val medicine: FullMedicine, val reminder: Reminder)

typealias scheduledReminderConsumerType = (ScheduledReminder, LocalDate, Double) -> Boolean

class SchedulingSimulator(medicines: List<FullMedicine>, recentReminders: List<ReminderEvent>, timeAccess: ReminderScheduler.TimeAccess) {
    var totalEvents = mutableListOf(*recentReminders.toTypedArray())
    var schedulingItems = medicines.map { it.reminders.map { reminder -> SchedulingItem(it, reminder) } }.flatten().filter { it.reminder.active }
    val schedulingFactory = SchedulingFactory()
    var currentDay: LocalDate = timeAccess.localDate()
    val timeAccess = object : ReminderScheduler.TimeAccess {
        override fun systemZone(): ZoneId {
            return timeAccess.systemZone()
        }

        override fun localDate(): LocalDate {
            return currentDay
        }
    }

    fun simulate(scheduledReminderConsumer: scheduledReminderConsumerType) {
        while (simulateDay(scheduledReminderConsumer)) {
            currentDay = currentDay.plusDays(1)
        }
    }

    private fun simulateDay(scheduledReminderConsumer: scheduledReminderConsumerType): Boolean {
        var continueSimulating = true
        for (schedulingItem in schedulingItems) {
            do {
                val nextScheduledTime = getNextScheduledTime(schedulingItem)
                if (nextScheduledTime != null) {
                    continueSimulating = processScheduledTime(schedulingItem, nextScheduledTime, scheduledReminderConsumer)
                }
            } while (nextScheduledTime != null && continueSimulating)
            if (!continueSimulating) {
                break
            }
        }
        return continueSimulating
    }

    private fun getNextScheduledTime(schedulingItem: SchedulingItem): Instant? {
        val scheduler = schedulingFactory.create(schedulingItem.reminder, totalEvents, timeAccess)
        var nextScheduledTime = scheduler.getNextScheduledTime()
        // Skip if not on current day
        if (nextScheduledTime?.atZone(timeAccess.systemZone())?.toLocalDate() != currentDay) {
            nextScheduledTime = null
        }
        return nextScheduledTime
    }

    private fun processScheduledTime(
        schedulingItem: SchedulingItem,
        nextScheduledTime: Instant,
        scheduledReminderConsumer: scheduledReminderConsumerType
    ): Boolean {
        val scheduledReminder = ScheduledReminder(schedulingItem.medicine, schedulingItem.reminder, nextScheduledTime)
        // Process stock
        doStockHandling(schedulingItem.medicine, scheduledReminder.reminder)
        // Notify consumer
        val continueSimulating = scheduledReminderConsumer(scheduledReminder, currentDay, schedulingItem.medicine.medicine.amount)
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(createReminderEvent(schedulingItem.reminder, nextScheduledTime))
        return continueSimulating
    }

    private fun doStockHandling(medicine: FullMedicine, reminder: Reminder) {
        val amount: Double? = MedicineHelper.parseAmount(reminder.amount)
        if (amount != null) {
            medicine.medicine.amount -= amount
            if (medicine.medicine.amount < 0) {
                medicine.medicine.amount = 0.0
            }
        }
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