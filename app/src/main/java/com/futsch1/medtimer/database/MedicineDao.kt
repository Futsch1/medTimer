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
abstract class MedicineDao {

    @Transaction
    open suspend fun decreaseStock(medicineId: Int, decreaseAmount: Double): FullMedicine? {
        val medicine = getOnlyMedicine(medicineId) ?: return null
        medicine.amount = maxOf(0.0, medicine.amount - decreaseAmount)
        updateMedicine(medicine)
        return getMedicine(medicineId)
    }

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract fun getMedicinesFlow(): Flow<List<FullMedicine>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    abstract suspend fun getMedicines(): List<FullMedicine>

    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    abstract suspend fun getOnlyMedicine(medicineId: Int): Medicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    abstract suspend fun getMedicine(medicineId: Int): FullMedicine?

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId ORDER BY timeInMinutes")
    abstract fun getRemindersFlow(medicineId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    abstract suspend fun getReminders(medicineId: Int): List<Reminder>

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    abstract suspend fun getReminder(reminderId: Int): Reminder?

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    abstract fun getReminderFlow(reminderId: Int): Flow<Reminder?>

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    abstract fun getMedicineFlow(medicineId: Int): Flow<FullMedicine?>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    abstract fun getReminderEventsFlowStartingFrom(fromTimestamp: Long, statusValues: List<ReminderStatus>): Flow<List<ReminderEvent>>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    abstract suspend fun getLimitedReminderEvents(fromTimestamp: Long, statusValues: List<ReminderStatus>): List<ReminderEvent>

    @Insert
    abstract suspend fun insertMedicine(medicine: Medicine): Long

    @Insert
    abstract suspend fun insertReminder(reminder: Reminder): Long

    @Update
    abstract suspend fun updateMedicine(medicine: Medicine)

    @Update
    abstract suspend fun updateReminder(reminder: Reminder)

    @Update
    abstract suspend fun updateReminders(reminders: List<Reminder>)

    @Delete
    abstract suspend fun deleteMedicine(medicine: Medicine)

    @Delete
    abstract suspend fun deleteReminder(reminder: Reminder)

    @Insert
    abstract suspend fun insertReminderEvent(reminderEvent: ReminderEvent): Long

    @Update
    abstract suspend fun updateReminderEvent(reminderEvent: ReminderEvent)

    @Update
    abstract suspend fun updateReminderEvents(reminderEvents: List<ReminderEvent>)

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId= :reminderEventId")
    abstract suspend fun getReminderEvent(reminderEventId: Int): ReminderEvent?

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId= :reminderEventId")
    abstract fun getReminderEventFlow(reminderEventId: Int): Flow<ReminderEvent?>

    @Query("DELETE FROM ReminderEvent")
    abstract suspend fun deleteReminderEvents()

    @Delete
    abstract suspend fun deleteReminderEvent(reminderEvent: ReminderEvent)

    @Query("DELETE FROM Reminder")
    abstract suspend fun deleteReminders()

    @Query("DELETE FROM Medicine")
    abstract suspend fun deleteMedicines()

    @Query("SELECT * FROM Reminder WHERE linkedReminderId= :reminderId")
    abstract suspend fun getLinkedReminders(reminderId: Int): List<Reminder>

    @Query("SELECT * FROM Tag")
    abstract fun getTagsFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM Tag WHERE name= :name")
    abstract suspend fun getTagByName(name: String): Tag?

    @Insert
    abstract suspend fun insertTag(tag: Tag): Long

    @Delete
    abstract suspend fun deleteTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMedicineToTag(medicineToTag: MedicineToTag)

    @Delete
    abstract suspend fun deleteMedicineToTag(medicineToTag: MedicineToTag)

    @Query("DELETE FROM MedicineToTag WHERE tagId= :tagId")
    abstract suspend fun deleteMedicineToTagForTag(tagId: Int)

    @Query("DELETE FROM MedicineToTag WHERE medicineId= :medicineId")
    abstract suspend fun deleteMedicineToTagForMedicine(medicineId: Int)

    @Query("DELETE FROM Tag")
    abstract suspend fun deleteTags()

    @Query("DELETE FROM MedicineToTag")
    abstract suspend fun deleteMedicineToTags()

    @Query("SELECT * FROM MedicineToTag")
    abstract fun getMedicineToTagsFlow(): Flow<List<MedicineToTag>>

    @Query("SELECT COUNT(*) FROM Tag")
    abstract suspend fun countTags(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Medicine")
    abstract suspend fun getHighestMedicineSortOrder(): Double

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC")
    abstract suspend fun getReminderEvents(reminderId: Int): List<ReminderEvent>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC LIMIT 1")
    abstract suspend fun getLastReminderEvent(reminderId: Int): ReminderEvent?

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId ORDER BY remindedTimestamp DESC LIMIT :limit")
    abstract suspend fun getLastReminderEvents(reminderId: Int, limit: Int): List<ReminderEvent>

    @Query("SELECT * FROM ReminderEvent WHERE reminderId= :reminderId AND remindedTimestamp= :remindedTimestamp")
    abstract suspend fun getReminderEvent(reminderId: Int, remindedTimestamp: Long): ReminderEvent?

    @Insert
    abstract suspend fun insertReminderEvents(reminderEvents: List<ReminderEvent>)

    @Insert
    abstract suspend fun insertReminders(reminders: List<Reminder>)

    @Update
    abstract suspend fun updateMedicines(medicines: List<Medicine>)
}
