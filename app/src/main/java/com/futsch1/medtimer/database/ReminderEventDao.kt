package com.futsch1.medtimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.futsch1.medtimer.database.ReminderEventEntity.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderEventDao {

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    abstract fun getAllFlowStartingFrom(fromTimestamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEventEntity>>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    abstract suspend fun getAllLimited(fromTimestamp: Long, statusValues: List<ReminderStatus>): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId = :reminderEventId")
    abstract suspend fun get(reminderEventId: Int): ReminderEventEntity?

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId = :reminderEventId")
    abstract fun getFlow(reminderEventId: Int): Flow<ReminderEventEntity?>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC")
    abstract suspend fun getAllByReminder(reminderId: Int): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC LIMIT 1")
    abstract suspend fun getLast(reminderId: Int): ReminderEventEntity?

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId ORDER BY remindedTimestamp DESC LIMIT :limit")
    abstract suspend fun getLastN(reminderId: Int, limit: Int): List<ReminderEventEntity>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId = :reminderId AND remindedTimestamp = :remindedTimestamp")
    abstract suspend fun get(reminderId: Int, remindedTimestamp: Long): ReminderEventEntity?

    @Insert
    abstract suspend fun create(reminderEvent: ReminderEventEntity): Long

    @Insert
    abstract suspend fun createAll(reminderEvents: List<ReminderEventEntity>)

    @Update
    abstract suspend fun update(reminderEvent: ReminderEventEntity)

    @Update
    abstract suspend fun updateAll(reminderEvents: List<ReminderEventEntity>)

    @Delete
    abstract suspend fun delete(reminderEvent: ReminderEventEntity)

    @Query("DELETE FROM ReminderEvent")
    abstract suspend fun deleteAll()
}
