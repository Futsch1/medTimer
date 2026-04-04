package com.futsch1.medtimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReminderDao {

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId ORDER BY timeInMinutes")
    abstract fun getAllFlow(medicineId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId = :medicineId")
    abstract suspend fun getAll(medicineId: Int): List<Reminder>

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    abstract suspend fun get(reminderId: Int): Reminder?

    @Query("SELECT * FROM Reminder WHERE reminderId = :reminderId")
    abstract fun getFlow(reminderId: Int): Flow<Reminder?>

    @Query("SELECT * FROM Reminder WHERE linkedReminderId = :reminderId")
    abstract suspend fun getLinked(reminderId: Int): List<Reminder>

    @Insert
    abstract suspend fun create(reminder: Reminder): Long

    @Insert
    abstract suspend fun createAll(reminders: List<Reminder>)

    @Update
    abstract suspend fun update(reminder: Reminder)

    @Update
    abstract suspend fun updateAll(reminders: List<Reminder>)

    @Delete
    abstract suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM Reminder")
    abstract suspend fun deleteAll()
}
