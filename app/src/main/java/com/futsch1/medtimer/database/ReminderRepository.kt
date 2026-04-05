package com.futsch1.medtimer.database

import com.futsch1.medtimer.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    fun getAllFlow(medicineId: Int): Flow<List<Reminder>> {
        return reminderDao.getAllFlow(medicineId).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun getAll(medicineId: Int): List<Reminder> {
        return reminderDao.getAll(medicineId).map { it.toModel() }
    }

    suspend fun get(reminderId: Int): Reminder? {
        return reminderDao.get(reminderId)?.toModel()
    }

    fun getFlow(reminderId: Int): Flow<Reminder?> {
        return reminderDao.getFlow(reminderId).map { it?.toModel() }
    }

    suspend fun getLinked(reminderId: Int): List<Reminder> {
        return reminderDao.getLinked(reminderId).map { it.toModel() }
    }

    suspend fun create(reminder: Reminder): Long {
        return reminderDao.create(reminder.toEntity())
    }

    suspend fun createAll(reminders: List<Reminder>) {
        reminderDao.createAll(reminders.map { it.toEntity() })
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder.toEntity())
    }

    suspend fun updateAll(reminders: List<Reminder>) {
        reminderDao.updateAll(reminders.map { it.toEntity() })
    }

    suspend fun delete(reminderId: Int) {
        reminderDao.get(reminderId)?.let { reminderDao.delete(it) }
    }

    suspend fun deleteAll() {
        reminderDao.deleteAll()
    }
}
