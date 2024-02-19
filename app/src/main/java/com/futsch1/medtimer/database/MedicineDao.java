package com.futsch1.medtimer.database;

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
    @Query("SELECT * FROM Medicine")
    LiveData<List<MedicineWithReminders>> getLiveMedicines();

    @Transaction
    @Query("SELECT * FROM Medicine")
    List<MedicineWithReminders> getMedicines();

    @Query("SELECT * FROM Medicine WHERE medicineId= :medicineId")
    Medicine getMedicine(int medicineId);

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId ORDER BY timeInMinutes")
    LiveData<List<Reminder>> getReminders(int medicineId);

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    Reminder getReminder(int reminderId);

    @Query("SELECT * FROM ReminderEvent ORDER BY remindedTimestamp DESC LIMIT :limit")
    LiveData<List<ReminderEvent>> getReminderEvents(int limit);

    @Query("SELECT * FROM ReminderEvent WHERE remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    LiveData<List<ReminderEvent>> getLiveReminderEvents(long fromTimestamp);

    @Query("SELECT * FROM ReminderEvent WHERE remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    List<ReminderEvent> getReminderEvents(long fromTimestamp);

    @Insert
    long insertMedicine(Medicine medicine);

    @Insert
    void insertReminder(Reminder reminder);

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
}
