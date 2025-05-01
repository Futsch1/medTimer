package com.futsch1.medtimer.database;

import static androidx.room.OnConflictStrategy.IGNORE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    LiveData<List<FullMedicine>> getLiveMedicines();

    @Transaction
    @Query("SELECT * FROM Medicine ORDER BY sortOrder")
    List<FullMedicine> getMedicines();

    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    Medicine getOnlyMedicine(int medicineId);

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    FullMedicine getMedicine(int medicineId);

    @Transaction
    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    LiveData<FullMedicine> getLiveMedicine(int medicineId);

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId ORDER BY timeInMinutes")
    LiveData<List<Reminder>> getLiveReminders(int medicineId);

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    List<Reminder> getReminders(int medicineId);

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    Reminder getReminder(int reminderId);

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    LiveData<List<ReminderEvent>> getLiveReminderEventsStartingFrom(long fromTimestamp, List<ReminderEvent.ReminderStatus> statusValues);

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    List<ReminderEvent> getLimitedReminderEvents(long fromTimestamp, List<ReminderEvent.ReminderStatus> statusValues);

    @Insert
    long insertMedicine(Medicine medicine);

    @Insert
    long insertReminder(Reminder reminder);

    @Update
    void updateMedicine(Medicine medicine);

    @Update
    void updateReminder(Reminder reminder);

    @Delete
    void deleteMedicine(Medicine medicine);

    @Delete
    void deleteReminder(Reminder reminder);

    @Insert
    long insertReminderEvent(ReminderEvent reminderEvent);

    @Update
    void updateReminderEvent(ReminderEvent reminderEvent);

    @Query("SELECT * FROM ReminderEvent WHERE reminderEventId= :reminderEventId")
    ReminderEvent getReminderEvent(int reminderEventId);

    @Query("DELETE FROM ReminderEvent")
    void deleteReminderEvents();

    @Delete
    void deleteReminderEvent(ReminderEvent reminderEvent);

    @Query("DELETE FROM Reminder")
    void deleteReminders();

    @Query("DELETE FROM Medicine")
    void deleteMedicines();

    @Query("SELECT * FROM Reminder WHERE linkedReminderId= :reminderId")
    List<Reminder> getLinkedReminders(int reminderId);

    @Transaction
    @Query("SELECT * FROM Tag")
    LiveData<List<Tag>> getLiveTags();

    @Query("SELECT * FROM Tag WHERE name= :name")
    Tag getTagByName(String name);

    @Insert
    long insertTag(Tag tag);

    @Delete
    void deleteTag(Tag tag);

    @Insert(onConflict = IGNORE)
    void insertMedicineToTag(MedicineToTag medicineToTag);

    @Delete
    void deleteMedicineToTag(MedicineToTag medicineToTag);

    @Query("DELETE FROM MedicineToTag WHERE tagId= :tagId")
    void deleteMedicineToTagForTag(int tagId);

    @Query("DELETE FROM MedicineToTag WHERE medicineId= :medicineId")
    void deleteMedicineToTagForMedicine(int medicineId);

    @Query("DELETE FROM Tag")
    void deleteTags();

    @Query("DELETE FROM MedicineToTag")
    void deleteMedicineToTags();

    @Query("SELECT * FROM MedicineToTag")
    LiveData<List<MedicineToTag>> getLiveMedicineToTags();

    @Query("SELECT COUNT(*) FROM Tag")
    int countTags();
}
