package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SchedulingItem(val medicine: Medicine, val reminder: Reminder)

typealias scheduledReminderConsumerType = (ScheduledReminder, LocalDate, Double) -> Boolean

class SchedulingSimulator(
    medicines: List<Medicine>,
    recentReminders: List<ReminderEvent>,
    timeAccess: TimeAccess,
    private val dataSource: PreferencesDataSource
) {
    val maxSimulationDays = 400

    var totalEvents = mutableListOf(*recentReminders.toTypedArray())
    var schedulingItems = medicines.flatMap { it.reminders.map { reminder -> SchedulingItem(it, reminder) } }.filter { it.reminder.active }
    private val medicineAmounts: MutableMap<Int, Double> = medicines.associate { it.id to it.amount }.toMutableMap()
    val schedulingFactory = SchedulingFactory()
    var currentDay: LocalDate = timeAccess.localDate()
    val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = timeAccess.systemZone()
        override fun localDate(): LocalDate = currentDay
        override fun now(): Instant = Instant.now()
    }

    fun simulate(scheduledReminderConsumer: scheduledReminderConsumerType) {
        val maxSimulationDay = currentDay.plusDays(maxSimulationDays.toLong())
        while (simulateDay(scheduledReminderConsumer) && currentDay < maxSimulationDay) {
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

    private fun currentMedicine(schedulingItem: SchedulingItem): Medicine =
        schedulingItem.medicine.copy(amount = medicineAmounts[schedulingItem.medicine.id] ?: schedulingItem.medicine.amount)

    private fun getNextScheduledTime(schedulingItem: SchedulingItem): Instant? {
        val scheduler = schedulingFactory.create(schedulingItem.reminder, currentMedicine(schedulingItem), totalEvents, timeAccess, dataSource)
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
        val medicine = currentMedicine(schedulingItem)
        val scheduledReminder = ScheduledReminder(medicine, schedulingItem.reminder, nextScheduledTime)
        // Process stock
        val updatedMedicine = doStockHandling(medicine, schedulingItem.reminder)
        medicineAmounts[schedulingItem.medicine.id] = updatedMedicine.amount
        // Notify consumer
        val continueSimulating = scheduledReminderConsumer(scheduledReminder, currentDay, updatedMedicine.amount)
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(createReminderEvent(schedulingItem.reminder, nextScheduledTime))
        return continueSimulating
    }

    private fun doStockHandling(medicine: Medicine, reminder: Reminder): Medicine {
        val amount: Double? = MedicineHelper.parseAmount(reminder.amount)

        return medicine.copy(amount = (medicine.amount - (amount ?: 0.0)).coerceAtLeast(0.0))
    }

    private fun createReminderEvent(
        reminder: Reminder,
        nextScheduledTime: Instant
    ): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            remindedTimestamp = nextScheduledTime,
            processedTimestamp = nextScheduledTime,
            reminderId = reminder.id,
            status = ReminderEvent.ReminderStatus.TAKEN
        )
        return reminderEvent
    }

}