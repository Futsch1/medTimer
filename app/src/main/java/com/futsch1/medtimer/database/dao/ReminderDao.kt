package com.futsch1.medtimer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.futsch1.medtimer.database.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId ORDER BY timeInMinutes")
    fun getAllFlow(medicineId: Int): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId")
    suspend fun getAll(medicineId: Int): List<ReminderEntity>

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    suspend fun get(reminderId: Int): ReminderEntity?

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    fun getFlow(reminderId: Int): Flow<ReminderEntity?>

    @Query("SELECT * FROM Reminder WHERE linkedReminderId = :reminderId")
    suspend fun getLinked(reminderId: Int): List<ReminderEntity>

    @Insert
    suspend fun create(reminder: ReminderEntity): Long

    @Insert
    suspend fun createAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Update
    suspend fun updateAll(reminders: List<ReminderEntity>)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM Reminder")
    suspend fun deleteAll()
}
