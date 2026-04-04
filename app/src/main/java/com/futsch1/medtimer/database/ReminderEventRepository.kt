package com.futsch1.medtimer.database

import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

private fun nowInSeconds() = Instant.now().toEpochMilli() / 1000

open class ReminderEventRepository(
    private val reminderEventDao: ReminderEventDao
) {
    fun getAllFlow(timeStamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEvent>> {
        return reminderEventDao.getAllFlowStartingFrom(timeStamp, statusValues)
    }

    suspend fun getAllWithoutDeleted(): List<ReminderEvent> {
        return reminderEventDao.getAllLimited(0L, statusValuesWithoutDelete)
    }

    suspend fun getAllWithoutDeletedAndAcknowledged(): List<ReminderEvent> {
        return reminderEventDao.getAllLimited(0L, statusValuesWithoutDeletedAndAcknowledged)
    }

    suspend fun getLastDays(days: Int): List<ReminderEvent> {
        val daysInSeconds = days.toLong() * 24 * 60 * 60
        val startTimestamp = nowInSeconds() - daysInSeconds
        return reminderEventDao.getAllLimited(startTimestamp, allStatusValues)
    }

    suspend fun getForScheduling(medicines: List<FullMedicine>): List<ReminderEvent> {
        val activeReminders = medicines.flatMap { it.reminders }.filter { it.active }
        return activeReminders.flatMap { getLastForScheduling(it.reminderId) }
    }

    private suspend fun getLastForScheduling(reminderId: Int): List<ReminderEvent> {
        var lastReminderEvents = reminderEventDao.getLastN(reminderId, 2)
        val now = nowInSeconds()
        if (lastReminderEvents.isNotEmpty() && lastReminderEvents
                .all { reminderEvent -> reminderEvent.remindedTimestamp > now }
        ) {
            lastReminderEvents = reminderEventDao.getAllByReminder(reminderId)
        }
        return lastReminderEvents
    }

    suspend fun getLast(reminderId: Int): ReminderEvent? {
        return reminderEventDao.getLast(reminderId)
    }

    suspend fun create(reminderEvent: ReminderEvent): Long {
        return reminderEventDao.create(reminderEvent)
    }

    suspend fun createAll(reminderEvents: List<ReminderEvent>) {
        reminderEventDao.createAll(reminderEvents)
    }

    suspend fun get(reminderEventId: Int): ReminderEvent? {
        return reminderEventDao.get(reminderEventId)
    }

    fun getFlow(reminderEventId: Int): Flow<ReminderEvent?> {
        return reminderEventDao.getFlow(reminderEventId)
    }

    suspend fun get(reminderId: Int, remindedTimestamp: Long): ReminderEvent? {
        return reminderEventDao.get(reminderId, remindedTimestamp)
    }

    suspend fun update(reminderEvent: ReminderEvent) {
        reminderEventDao.update(reminderEvent)
    }

    suspend fun updateAll(reminderEvents: List<ReminderEvent>) {
        reminderEventDao.updateAll(reminderEvents)
    }

    suspend fun delete(reminderEvent: ReminderEvent) {
        reminderEventDao.delete(reminderEvent)
    }

    suspend fun deleteAll() {
        reminderEventDao.deleteAll()
    }
}
