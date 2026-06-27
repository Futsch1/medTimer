package com.futsch1.medtimer.feature.reminders.scheduling

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.feature.reminders.TimeAccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

typealias ScheduledReminderConsumer = (ScheduledReminder, LocalDate) -> Boolean


class LastEventPerReminder(initialReminderEvents: List<ReminderEvent>) {
    private val lastReminderEvents = mutableMapOf<Int, ReminderEvent>()

    init {
        for (reminderEvent in initialReminderEvents) {
            val prevReminderEvent = lastReminderEvents[reminderEvent.reminderId]
            if (prevReminderEvent == null || prevReminderEvent.remindedTimestamp < reminderEvent.remindedTimestamp) {
                lastReminderEvents[reminderEvent.reminderId] = reminderEvent
            }
        }
    }

    // Unconditional: DB may pre-schedule future events; the simulation's synthetic event must always win
    fun add(reminderEvent: ReminderEvent) {
        lastReminderEvents[reminderEvent.reminderId] = reminderEvent
    }

    fun get(): List<ReminderEvent> {
        return lastReminderEvents.values.toList()
    }
}

class SchedulingSimulator(
    medicines: List<Medicine>,
    recentReminderEvents: List<ReminderEvent>,
    timeAccess: TimeAccess,
    private val dataSource: PreferencesDataSource
) {
    val maxSimulationDays = 400

    var totalEvents = LastEventPerReminder(recentReminderEvents)
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
        for (medicine in medicines.values) {
            do {
                val eventsSnapshot = totalEvents.get()
                var earliest: Pair<Reminder, Instant>? = null
                for (reminder in medicine.reminders) {
                    val nextForReminder =
                        getNextScheduledTime(medicine, reminder, eventsSnapshot) ?: continue
                    if (earliest == null || nextForReminder < earliest.second) {
                        earliest = reminder to nextForReminder
                    }
                }
                val next = earliest ?: break
                if (!processScheduledTime(next, medicine, scheduledReminderConsumer)) {
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

    private fun processScheduledTime(
        reminderAndInstant: Pair<Reminder, Instant>,
        medicine: Medicine,
        scheduledReminderConsumer: ScheduledReminderConsumer
    ): Boolean {
        val (stockBefore, stockAfter) = doStockHandling(medicine, reminderAndInstant.first)
        // Notify consumer
        val continueSimulating =
            scheduledReminderConsumer(
                ScheduledReminder(medicine, reminderAndInstant.first, reminderAndInstant.second, stockBefore, stockAfter),
                currentDay
            )
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(createReminderEvent(reminderAndInstant))
        return continueSimulating
    }

    private fun doStockHandling(medicine: Medicine, reminder: Reminder): Pair<Double, Double> {
        val stockBefore = medicines[medicine.id]?.amount ?: 0.0
        return if (stockBefore > 0.0) {
            val reminderAmount: Double =
                MedicineHelper.parseAmount(reminder.amount) ?: 0.0
            val stockAfter = (stockBefore - reminderAmount).coerceAtLeast(0.0)

            medicines[medicine.id] = medicine.copy(
                amount = stockAfter
            )
            stockBefore to stockAfter
        } else {
            0.0 to 0.0
        }
    }

    private fun createReminderEvent(
        reminderAndInstant: Pair<Reminder, Instant>
    ): ReminderEvent {
        val reminderEvent = ReminderEvent.default().copy(
            remindedTimestamp = reminderAndInstant.second,
            processedTimestamp = reminderAndInstant.second,
            reminderId = reminderAndInstant.first.id,
            status = ReminderEvent.ReminderStatus.TAKEN
        )
        return reminderEvent
    }

}