package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private fun nowInSeconds() = Instant.now().toEpochMilli() / 1000

class ReminderEventRepositoryImpl(
    private val reminderEventDao: ReminderEventDao
) : ReminderEventRepository {
    override fun getAllFlow(timeStamp: Long, statusValues: List<ReminderEvent.ReminderStatus>): Flow<List<ReminderEvent>> {
        val entityStatusValues = statusValues.map { it.toEntity() }
        return reminderEventDao.getAllFlowStartingFrom(timeStamp, entityStatusValues).map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun getAllWithoutDeleted(): List<ReminderEvent> {
        return reminderEventDao.getAllLimited(0L, statusValuesWithoutDelete).map { it.toModel() }
    }

    override suspend fun getAllWithoutDeletedAndAcknowledged(): List<ReminderEvent> {
        return reminderEventDao.getAllLimited(0L, statusValuesWithoutDeletedAndAcknowledged).map { it.toModel() }
    }

    override suspend fun getLastDays(days: Int): List<ReminderEvent> {
        val daysInSeconds = days.toLong() * 24 * 60 * 60
        val startTimestamp = nowInSeconds() - daysInSeconds
        return reminderEventDao.getAllLimited(startTimestamp, allStatusValues).map { it.toModel() }
    }

    override suspend fun getForScheduling(medicines: List<Medicine>): List<ReminderEvent> {
        val activeReminders = medicines.flatMap { it.reminders }.filter { it.active }
        return activeReminders.flatMap { getLastForScheduling(it.id) }
    }

    private suspend fun getLastForScheduling(reminderId: Int): List<ReminderEvent> {
        var lastReminderEvents = reminderEventDao.getLastN(reminderId, 2).map { it.toModel() }
        val now = Instant.now()
        if (lastReminderEvents.isNotEmpty() && lastReminderEvents
                .all { reminderEvent -> reminderEvent.remindedTimestamp > now }
        ) {
            lastReminderEvents = reminderEventDao.getAllByReminder(reminderId).map { it.toModel() }
        }
        return lastReminderEvents
    }

    override suspend fun getLast(reminderId: Int): ReminderEvent? {
        return reminderEventDao.getLast(reminderId)?.toModel()
    }

    override suspend fun create(reminderEvent: ReminderEvent): ReminderEvent {
        val reminderEventId = reminderEventDao.create(reminderEvent.toEntity())
        return reminderEvent.copy(reminderEventId = reminderEventId.toInt())
    }

    override suspend fun createAll(reminderEvents: List<ReminderEvent>) {
        reminderEventDao.createAll(reminderEvents.map { it.toEntity() })
    }

    override suspend fun get(reminderEventId: Int): ReminderEvent? {
        return reminderEventDao.get(reminderEventId)?.toModel()
    }

    override fun getFlow(reminderEventId: Int): Flow<ReminderEvent?> {
        return reminderEventDao.getFlow(reminderEventId).map { it?.toModel() }
    }

    override suspend fun get(reminderId: Int, remindedTimestamp: Long): ReminderEvent? {
        return reminderEventDao.get(reminderId, remindedTimestamp)?.toModel()
    }

    override suspend fun update(reminderEvent: ReminderEvent) {
        reminderEventDao.update(reminderEvent.toEntity())
    }

    override suspend fun updateAll(reminderEvents: List<ReminderEvent>) {
        reminderEventDao.updateAll(reminderEvents.map { it.toEntity() })
    }

    override suspend fun delete(reminderEvent: ReminderEvent) {
        reminderEventDao.delete(reminderEvent.toEntity())
    }

    override suspend fun deleteAll() {
        reminderEventDao.deleteAll()
    }

    override suspend fun decreaseRepeats(reminderEventId: Int) {
        reminderEventDao.decreaseRepeats(reminderEventId)
    }
}
