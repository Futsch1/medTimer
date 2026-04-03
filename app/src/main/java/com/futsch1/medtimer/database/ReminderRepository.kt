package com.futsch1.medtimer.database

import kotlinx.coroutines.flow.Flow

open class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    fun getAllFlow(medicineId: Int): Flow<List<Reminder>> {
        return reminderDao.getAllFlow(medicineId)
    }

    suspend fun getAll(medicineId: Int): List<Reminder> {
        return reminderDao.getAll(medicineId)
    }

    suspend fun get(reminderId: Int): Reminder? {
        return reminderDao.get(reminderId)
    }

    fun getFlow(reminderId: Int): Flow<Reminder?> {
        return reminderDao.getFlow(reminderId)
    }

    suspend fun getLinked(reminderId: Int): List<Reminder> {
        return reminderDao.getLinked(reminderId)
    }

    suspend fun create(reminder: Reminder): Long {
        return reminderDao.create(reminder)
    }

    suspend fun createAll(reminders: List<Reminder>) {
        reminderDao.createAll(reminders)
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
    }

    suspend fun updateAll(reminders: List<Reminder>) {
        reminderDao.updateAll(reminders)
    }

    suspend fun delete(reminderId: Int) {
        reminderDao.get(reminderId)?.let { reminderDao.delete(it) }
    }

    suspend fun deleteAll() {
        reminderDao.deleteAll()
    }
}
