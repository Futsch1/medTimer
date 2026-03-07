package com.futsch1.medtimer.database

import androidx.lifecycle.LiveData
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
    fun getLiveMedicines(): LiveData<List<FullMedicine>>

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    fun getMedicines(): List<FullMedicine>

    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getOnlyMedicine(medicineId: Int): Medicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getMedicine(medicineId: Int): FullMedicine?

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getLiveMedicine(medicineId: Int): LiveData<FullMedicine?>

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId ORDER BY timeInMinutes")
    fun getLiveReminders(medicineId: Int): LiveData<List<Reminder>>

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    fun getReminders(medicineId: Int): List<Reminder>

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    fun getReminder(reminderId: Int): Reminder?

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    fun getReminderFlow(reminderId: Int): Flow<Reminder?>

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    fun getMedicineFlow(medicineId: Int): Flow<FullMedicine?>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    fun getLiveReminderEventsStartingFrom(fromTimestamp: Long, statusValues: List<ReminderStatus>): LiveData<List<ReminderEvent>>

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    fun getLimitedReminderEvents(fromTimestamp: Long, statusValues: List<ReminderStatus>): List<ReminderEvent>

    @Insert
    fun insertMedicine(medicine: Medicine): Long

    @Insert
    fun insertReminder(reminder: Reminder): Long

    @Update
    fun updateMedicine(medicine: Medicine)

    @Update
    fun updateReminder(reminder: Reminder)

    @Delete
    fun deleteMedicine(medicine: Medicine)

    @Delete
    fun deleteReminder(reminder: Reminder)

    @Insert
    fun insertReminderEvent(reminderEvent: ReminderEvent): Long

    @Update
    fun updateReminderEvent(reminderEvent: ReminderEvent)

    @Update
    fun updateReminderEvents(reminderEvents: List<ReminderEvent>)

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId= :reminderEventId")
    fun getReminderEvent(reminderEventId: Int): ReminderEvent?

    @Query("DELETE FROM ReminderEvent")
    fun deleteReminderEvents()

    @Delete
    fun deleteReminderEvent(reminderEvent: ReminderEvent)

    @Query("DELETE FROM Reminder")
    fun deleteReminders()

    @Query("DELETE FROM Medicine")
    fun deleteMedicines()

    @Query("SELECT * FROM Reminder WHERE linkedReminderId= :reminderId")
    fun getLinkedReminders(reminderId: Int): List<Reminder>

    @Transaction
    @Query("SELECT * FROM Tag")
    fun getLiveTags(): LiveData<List<Tag>>

    @Query("SELECT * FROM Tag WHERE name= :name")
    fun getTagByName(name: String): Tag?

    @Insert
    fun insertTag(tag: Tag): Long

    @Delete
    fun deleteTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMedicineToTag(medicineToTag: MedicineToTag)

    @Delete
    fun deleteMedicineToTag(medicineToTag: MedicineToTag)

    @Query("DELETE FROM MedicineToTag WHERE tagId= :tagId")
    fun deleteMedicineToTagForTag(tagId: Int)

    @Query("DELETE FROM MedicineToTag WHERE medicineId= :medicineId")
    fun deleteMedicineToTagForMedicine(medicineId: Int)

    @Query("DELETE FROM Tag")
    fun deleteTags()

    @Query("DELETE FROM MedicineToTag")
    fun deleteMedicineToTags()

    @get:Query("SELECT * FROM MedicineToTag")
    val liveMedicineToTags: LiveData<List<MedicineToTag>>

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
    fun insertReminderEvents(reminderEvents: List<ReminderEvent>)

    @Insert
    fun insertReminders(reminders: List<Reminder>)

    @Update
    fun updateMedicines(medicines: List<Medicine>)
}
