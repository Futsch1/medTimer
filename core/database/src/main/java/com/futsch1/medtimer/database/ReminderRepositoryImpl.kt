package com.futsch1.medtimer.database

import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.toModel.toEntity
import com.futsch1.medtimer.database.toModel.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {
    override fun getAllFlow(medicineId: Int): Flow<List<Reminder>> {
        return reminderDao.getAllFlow(medicineId).map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun getAll(medicineId: Int): List<Reminder> {
        return reminderDao.getAll(medicineId).map { it.toModel() }
    }

    override suspend operator fun get(reminderId: Int): Reminder? {
        return reminderDao[reminderId]?.toModel()
    }

    override fun getFlow(reminderId: Int): Flow<Reminder?> {
        return reminderDao.getFlow(reminderId).map { it?.toModel() }
    }

    override suspend fun getLinked(reminderId: Int): List<Reminder> {
        return reminderDao.getLinked(reminderId).map { it.toModel() }
    }

    override suspend fun create(reminder: Reminder): Int {
        return reminderDao.create(reminder.toEntity()).toInt()
    }

    override suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder.toEntity())
    }

    override suspend fun updateAll(reminders: List<Reminder>) {
        reminderDao.updateAll(reminders.map { it.toEntity() })
    }

    override suspend fun delete(reminderId: Int) {
        reminderDao[reminderId]?.let { reminderDao.delete(it) }
    }

    override suspend fun deleteAll() {
        reminderDao.deleteAll()
    }
}
