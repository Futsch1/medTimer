package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    fun getAllFlow(medicineId: Int): Flow<List<ReminderEntity>> {
        return reminderDao.getAllFlow(medicineId)
    }

    suspend fun getAll(medicineId: Int): List<ReminderEntity> {
        return reminderDao.getAll(medicineId)
    }

    suspend fun get(reminderId: Int): ReminderEntity? {
        return reminderDao.get(reminderId)
    }

    fun getFlow(reminderId: Int): Flow<ReminderEntity?> {
        return reminderDao.getFlow(reminderId)
    }

    suspend fun getLinked(reminderId: Int): List<ReminderEntity> {
        return reminderDao.getLinked(reminderId)
    }

    suspend fun create(reminder: ReminderEntity): Long {
        return reminderDao.create(reminder)
    }

    suspend fun createAll(reminders: List<ReminderEntity>) {
        reminderDao.createAll(reminders)
    }

    suspend fun update(reminder: ReminderEntity) {
        reminderDao.update(reminder)
    }

    suspend fun updateAll(reminders: List<ReminderEntity>) {
        reminderDao.updateAll(reminders)
    }

    suspend fun delete(reminderId: Int) {
        reminderDao.get(reminderId)?.let { reminderDao.delete(it) }
    }

    suspend fun deleteAll() {
        reminderDao.deleteAll()
    }
}
