package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.futsch1.medtimer.database.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderDao {

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId ORDER BY timeInMinutes")
    abstract fun getAllFlow(medicineId: Int): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId")
    abstract suspend fun getAll(medicineId: Int): List<ReminderEntity>

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    abstract suspend fun get(reminderId: Int): ReminderEntity?

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    abstract fun getFlow(reminderId: Int): Flow<ReminderEntity?>

    @Query("SELECT * FROM Reminder WHERE linkedReminderId = :reminderId")
    abstract suspend fun getLinked(reminderId: Int): List<ReminderEntity>

    @Insert
    abstract suspend fun create(reminder: ReminderEntity): Long

    @Insert
    abstract suspend fun createAll(reminders: List<ReminderEntity>)

    @Update
    abstract suspend fun update(reminder: ReminderEntity)

    @Update
    abstract suspend fun updateAll(reminders: List<ReminderEntity>)

    @Delete
    abstract suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM Reminder")
    abstract suspend fun deleteAll()
}
