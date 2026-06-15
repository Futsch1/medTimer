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

typealias ScheduledReminderConsumer = (ScheduledReminder, LocalDate, Double) -> Boolean


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
                var earliest: ScheduledReminder? = null
                for (reminder in medicine.reminders) {
                    val nextForReminder =
                        getNextScheduledTime(medicine, reminder, eventsSnapshot) ?: continue
                    if (earliest == null || nextForReminder < earliest.timestamp) {
                        earliest = ScheduledReminder(medicine, reminder, nextForReminder)
                    }
                }
                val next = earliest ?: break
                if (!processScheduledTime(next, scheduledReminderConsumer)) {
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
        scheduledReminder: ScheduledReminder,
        scheduledReminderConsumer: ScheduledReminderConsumer
    ): Boolean {
        val amount = doStockHandling(scheduledReminder)
        // Notify consumer
        val continueSimulating =
            scheduledReminderConsumer(
                scheduledReminder,
                currentDay,
                amount
            )
        // Add the simulated event to make sure it is considered in the next scheduling call
        totalEvents.add(
            createReminderEvent(
                scheduledReminder.reminder,
                scheduledReminder.timestamp
            )
        )
        return continueSimulating
    }

    private fun doStockHandling(scheduledReminder: ScheduledReminder): Double {
        val currentMedicineAmount = medicines[scheduledReminder.medicine.id]?.amount ?: 0.0
        return if (currentMedicineAmount > 0.0) {
            val reminderAmount: Double =
                MedicineHelper.parseAmount(scheduledReminder.reminder.amount) ?: 0.0
            val amount = (currentMedicineAmount - reminderAmount).coerceAtLeast(0.0)

            medicines[scheduledReminder.medicine.id] = scheduledReminder.medicine.copy(
                amount = amount
            )
            amount
        } else {
            0.0
        }
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