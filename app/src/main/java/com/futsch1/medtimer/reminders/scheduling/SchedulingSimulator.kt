package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SchedulingItem(val medicine: FullMedicineEntity, val reminder: ReminderEntity)

typealias scheduledReminderConsumerType = (ScheduledReminder, LocalDate, Double) -> Boolean

class SchedulingSimulator(
    medicines: List<FullMedicineEntity>,
    recentReminders: List<ReminderEvent>,
    timeAccess: TimeAccess,
    private val dataSource: PreferencesDataSource
) {
    val maxSimulationDays = 400

    var totalEvents = mutableListOf(*recentReminders.toTypedArray())
    var schedulingItems = medicines.flatMap { it.reminders.map { reminder -> SchedulingItem(it, reminder) } }.filter { it.reminder.active }
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

    private fun getNextScheduledTime(schedulingItem: SchedulingItem): Instant? {
        val scheduler = schedulingFactory.create(schedulingItem.reminder, schedulingItem.medicine.medicine, totalEvents, timeAccess, dataSource)
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
        val scheduledReminder = ScheduledReminder(schedulingItem.medicine, schedulingItem.reminder.toModel(), nextScheduledTime)
        // Process stock
        doStockHandling(schedulingItem.medicine, schedulingItem.reminder)
        // Notify consumer
        val continueSimulating = scheduledReminderConsumer(scheduledReminder, currentDay, schedulingItem.medicine.medicine.amount)
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(createReminderEvent(schedulingItem.reminder, nextScheduledTime))
        return continueSimulating
    }

    private fun doStockHandling(medicine: FullMedicineEntity, reminder: ReminderEntity) {
        val amount: Double? = MedicineHelper.parseAmount(reminder.amount)
        if (amount != null) {
            medicine.medicine.amount -= amount
            if (medicine.medicine.amount < 0) {
                medicine.medicine.amount = 0.0
            }
        }
    }

    private fun createReminderEvent(
        reminder: ReminderEntity,
        nextScheduledTime: Instant
    ): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            remindedTimestamp = nextScheduledTime,
            processedTimestamp = nextScheduledTime,
            reminderId = reminder.reminderId,
            status = ReminderEvent.ReminderStatus.TAKEN
        )
        return reminderEvent
    }

}