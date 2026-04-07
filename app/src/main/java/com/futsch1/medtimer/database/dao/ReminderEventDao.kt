package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventEntity.ReminderEntityStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderEventDao {

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    fun getAllFlowStartingFrom(fromTimestamp: Long, statusValues: List<ReminderEntityStatus>): Flow<List<ReminderEventEntity>>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    suspend fun getAllLimited(fromTimestamp: Long, statusValues: List<ReminderEntityStatus>): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId = :reminderEventId")
    suspend fun get(reminderEventId: Int): ReminderEventEntity?

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId = :reminderEventId")
    fun getFlow(reminderEventId: Int): Flow<ReminderEventEntity?>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC")
    suspend fun getAllByReminder(reminderId: Int): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC LIMIT 1")
    suspend fun getLast(reminderId: Int): ReminderEventEntity?

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC LIMIT :limit")
    suspend fun getLastN(reminderId: Int, limit: Int): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId AND remindedTimestamp = :remindedTimestamp")
    suspend fun get(reminderId: Int, remindedTimestamp: Long): ReminderEventEntity?

    @Insert
    suspend fun create(reminderEvent: ReminderEventEntity): Long

    @Insert
    suspend fun createAll(reminderEvents: List<ReminderEventEntity>)

    @Update
    suspend fun update(reminderEvent: ReminderEventEntity)

    @Update
    suspend fun updateAll(reminderEvents: List<ReminderEventEntity>)

    @Delete
    suspend fun delete(reminderEvent: ReminderEventEntity)

    @Query("DELETE FROM ReminderEvent")
    suspend fun deleteAll()
}
