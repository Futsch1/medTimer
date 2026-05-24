package com.futsch1.medtimer.core.domain.repository

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import kotlinx.coroutines.flow.Flow

interface ReminderEventRepository {
    fun getAllFlow(
        timeStamp: Long,
        statusValues: List<ReminderEvent.ReminderStatus>
    ): Flow<List<ReminderEvent>>

    suspend fun getAllWithoutDeleted(): List<ReminderEvent>
    suspend fun getAllWithoutDeletedAndAcknowledged(): List<ReminderEvent>
    suspend fun getLastDays(days: Int): List<ReminderEvent>
    suspend fun getForScheduling(medicines: List<Medicine>): List<ReminderEvent>
    suspend fun getLast(reminderId: Int): ReminderEvent?
    suspend fun create(reminderEvent: ReminderEvent): ReminderEvent
    suspend fun createAll(reminderEvents: List<ReminderEvent>)
    suspend operator fun get(reminderEventId: Int): ReminderEvent?
    fun getFlow(reminderEventId: Int): Flow<ReminderEvent?>
    suspend operator fun get(reminderId: Int, remindedTimestamp: Long): ReminderEvent?
    suspend fun update(reminderEvent: ReminderEvent)
    suspend fun updateAll(reminderEvents: List<ReminderEvent>)
    suspend fun delete(reminderEvent: ReminderEvent)
    suspend fun deleteAll()
    suspend fun decreaseRepeats(reminderEventId: Int)
}
