package com.futsch1.medtimer.feature.reminders.scheduling

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.feature.reminders.TimeAccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

typealias ScheduledReminderConsumer = (SimulatedReminder, LocalDate) -> Boolean


class LastEventsPerReminder(initialReminderEvents: List<ReminderEvent>) {
    private val latestPastEvent = mutableMapOf<Int, ReminderEvent>()
    private val futureEvents = mutableMapOf<Int, MutableList<ReminderEvent>>()

    init {
        for (reminderEvent in initialReminderEvents) {
            futureEvents.getOrPut(reminderEvent.reminderId) { mutableListOf() }.add(reminderEvent)
        }
    }

    fun advanceTo(endOfCurrentDay: Instant) {
        for ((reminderId, events) in futureEvents) {
            val promoted = events.filter { it.remindedTimestamp < endOfCurrentDay }
            if (promoted.isNotEmpty()) {
                val maxPromoted = promoted.maxBy { it.remindedTimestamp }
                val current = latestPastEvent[reminderId]
                if (current == null || current.remindedTimestamp < maxPromoted.remindedTimestamp) {
                    latestPastEvent[reminderId] = maxPromoted
                }
                events.removeAll(promoted.toSet())
            }
        }
    }

    fun add(reminderEvent: ReminderEvent) {
        val current = latestPastEvent[reminderEvent.reminderId]
        if (current == null || current.remindedTimestamp < reminderEvent.remindedTimestamp) {
            latestPastEvent[reminderEvent.reminderId] = reminderEvent
        }
    }

    fun get(): List<ReminderEvent> {
        return latestPastEvent.values.toList()
    }
}

class SchedulingSimulator(
    medicines: List<Medicine>,
    recentReminderEvents: List<ReminderEvent>,
    timeAccess: TimeAccess,
    private val dataSource: PreferencesDataSource
) {
    val maxSimulationDays = 400

    var totalEvents = LastEventsPerReminder(recentReminderEvents)
    val medicines =
        medicines.associateBy(
            { it.id },
            { it.copy(reminders = it.reminders.filter { iter -> iter.active }) }).toMutableMap()

    val schedulingFactory = SchedulingFactory()
    var endOfCurrentDay: Instant = Instant.EPOCH
    var currentDay: LocalDate = timeAccess.localDate()
        set(value) {
            endOfCurrentDay =
                TimeHelper.instantAtStartOfDay(value.plusDays(1), timeAccess.systemZone())
            field = value
        }
    val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = timeAccess.systemZone()
        override fun localDate(): LocalDate = currentDay
        override fun now(): Instant = Instant.now()
    }

    init {
        currentDay = timeAccess.localDate()
    }

    suspend fun simulate(scheduledReminderConsumer: ScheduledReminderConsumer) {
        val context = currentCoroutineContext()
        val maxSimulationDay = currentDay.plusDays(maxSimulationDays.toLong())
        while (simulateDay(scheduledReminderConsumer) && currentDay < maxSimulationDay) {
            currentDay = currentDay.plusDays(1)
            context.ensureActive()
        }
    }

    private fun simulateDay(scheduledReminderConsumer: ScheduledReminderConsumer): Boolean {
        totalEvents.advanceTo(endOfCurrentDay)
        for (medicine in medicines.values) {
            do {
                val eventsSnapshot = totalEvents.get()
                var earliest: ScheduledReminder? = null
                for (reminder in medicine.reminders) {
                    val nextForReminder =
                        getNextScheduledTime(medicine, reminder, eventsSnapshot) ?: continue
                    if (earliest == null || nextForReminder < earliest.timestamp) {
                        earliest = ScheduledReminder(medicine, reminder, nextForReminder)
                    }
                }
                val next = earliest ?: break
                if (!processScheduledReminder(next, scheduledReminderConsumer)) {
                    return false
                }
            } while (true)
        }
        return true
    }

    private fun getNextScheduledTime(
        medicine: Medicine,
        reminder: Reminder,
        eventsSnapshot: List<ReminderEvent>
    ): Instant? {
        val scheduler =
            schedulingFactory.create(reminder, medicine, eventsSnapshot, timeAccess, dataSource)
        var nextScheduledTime = scheduler.getNextScheduledTime()
        // Skip if not on current day
        if ((nextScheduledTime ?: endOfCurrentDay) >= endOfCurrentDay) {
            nextScheduledTime = null
        }
        return nextScheduledTime
    }

    private fun processScheduledReminder(
        scheduledReminder: ScheduledReminder,
        scheduledReminderConsumer: ScheduledReminderConsumer
    ): Boolean {
        val (stockBefore, stockAfter) = doStockHandling(scheduledReminder)
        // Notify consumer
        val continueSimulating =
            scheduledReminderConsumer(
                SimulatedReminder(scheduledReminder, stockBefore, stockAfter),
                currentDay
            )
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(createReminderEvent(scheduledReminder))
        return continueSimulating
    }

    private fun doStockHandling(scheduledReminder: ScheduledReminder): Pair<Double, Double> {
        val stockBefore = medicines[scheduledReminder.medicine.id]?.amount ?: 0.0
        return if (stockBefore > 0.0) {
            val reminderAmount: Double =
                MedicineHelper.parseAmount(scheduledReminder.reminder.amount) ?: 0.0
            val stockAfter = (stockBefore - reminderAmount).coerceAtLeast(0.0)

            medicines[scheduledReminder.medicine.id] = scheduledReminder.medicine.copy(
                amount = stockAfter
            )
            stockBefore to stockAfter
        } else {
            0.0 to 0.0
        }
    }

    private fun createReminderEvent(
        scheduledReminder: ScheduledReminder
    ): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            remindedTimestamp = scheduledReminder.timestamp,
            processedTimestamp = scheduledReminder.timestamp,
            reminderId = scheduledReminder.reminder.id,
            status = ReminderEvent.ReminderStatus.TAKEN
        )
        return reminderEvent
    }

}