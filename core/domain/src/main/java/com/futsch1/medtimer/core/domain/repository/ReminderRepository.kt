package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllFlow(medicineId: Int): Flow<List<Reminder>>
    suspend fun getAll(medicineId: Int): List<Reminder>
    suspend fun get(reminderId: Int): Reminder?
    fun getFlow(reminderId: Int): Flow<Reminder?>
    suspend fun getLinked(reminderId: Int): List<Reminder>
    suspend fun create(reminder: Reminder): Int
    suspend fun update(reminder: Reminder)
    suspend fun updateAll(reminders: List<Reminder>)
    suspend fun delete(reminderId: Int)
    suspend fun deleteAll()
}
