package com.futsch1.medtimer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    fun getMedicinesFlow(): Flow<List<FullMedicine>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    fun getMedicines(): List<FullMedicine>

    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    suspend fun getOnlyMedicine(medicineId: Int): Medicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getMedicine(medicineId: Int): FullMedicine?
    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId ORDER BY timeInMinutes")
    fun getRemindersFlow(medicineId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    fun getReminders(medicineId: Int): List<Reminder>

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    suspend fun getReminder(reminderId: Int): Reminder?

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    fun getReminderFlow(reminderId: Int): Flow<Reminder?>

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getMedicineFlow(medicineId: Int): Flow<FullMedicine?>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    fun getReminderEventsFlowStartingFrom(fromTimestamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEvent>>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    fun getLimitedReminderEvents(fromTimestamp: Long, statusValues: List<ReminderStatus>): List<ReminderEvent>

    @Insert
    suspend fun insertMedicine(medicine: Medicine): Long

    @Insert
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Delete
    fun deleteReminder(reminder: Reminder)

    @Insert
    fun insertReminderEvent(reminderEvent: ReminderEvent): Long

    @Update
    suspend fun updateReminderEvent(reminderEvent: ReminderEvent)

    @Update
    fun updateReminderEvents(reminderEvents: List<ReminderEvent>)

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId= :reminderEventId")
    fun getReminderEvent(reminderEventId: Int): ReminderEvent?

    @Query("DELETE FROM ReminderEvent")
    suspend fun deleteReminderEvents()

    @Delete
    suspend fun deleteReminderEvent(reminderEvent: ReminderEvent)

    @Query("DELETE FROM Reminder")
    suspend fun deleteReminders()

    @Query("DELETE FROM Medicine")
    suspend fun deleteMedicines()

    @Query("SELECT * FROM Reminder WHERE linkedReminderId= :reminderId")
    fun getLinkedReminders(reminderId: Int): List<Reminder>

    @Transaction
    @Query("SELECT * FROM Tag")
    fun getTagsFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM Tag WHERE name= :name")
    fun getTagByName(name: String): Tag?

    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedicineToTag(medicineToTag: MedicineToTag)

    @Delete
    suspend fun deleteMedicineToTag(medicineToTag: MedicineToTag)

    @Query("DELETE FROM MedicineToTag WHERE tagId= :tagId")
    suspend fun deleteMedicineToTagForTag(tagId: Int)

    @Query("DELETE FROM MedicineToTag WHERE medicineId= :medicineId")
    suspend fun deleteMedicineToTagForMedicine(medicineId: Int)

    @Query("DELETE FROM Tag")
    suspend fun deleteTags()

    @Query("DELETE FROM MedicineToTag")
    suspend fun deleteMedicineToTags()

    @get:Query("SELECT * FROM MedicineToTag")
    val medicineToTagsFlow: Flow<List<MedicineToTag>>

    @Query("SELECT COUNT(*) FROM Tag")
    fun countTags(): Int

    @get:Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Medicine")
    val highestMedicineSortOrder: Double

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC")
    fun getReminderEvents(reminderId: Int): List<ReminderEvent>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC LIMIT 1")
    fun getLastReminderEvent(reminderId: Int): ReminderEvent?

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC LIMIT :limit")
    fun getLastReminderEvents(reminderId: Int, limit: Int): List<ReminderEvent>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId AND remindedTimestamp= :remindedTimestamp")
    fun getReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEvent?

    @Insert
    suspend fun insertReminderEvents(reminderEvents: List<ReminderEvent>)

    @Insert
    suspend fun insertReminders(reminders: List<Reminder>)

    @Update
    fun updateMedicines(medicines: List<Medicine>)
}
