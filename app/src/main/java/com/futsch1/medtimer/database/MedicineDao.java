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
    LiveData<List<Reminder>> getLiveReminders(int medicineId);

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    List<Reminder> getReminders(int medicineId);

    @Query("SELECT * FROM Reminder WHERE reminderId= :reminderId")
    Reminder getReminder(int reminderId);

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) ORDER BY remindedTimestamp DESC LIMIT :limit")
    LiveData<List<ReminderEvent>> getLiveReminderEvents(int limit, List<ReminderEvent.ReminderStatus> statusValues);

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp DESC")
    LiveData<List<ReminderEvent>> getLiveReminderEventsStartingFrom(long fromTimestamp, List<ReminderEvent.ReminderStatus> statusValues);

    @Query("SELECT * FROM ReminderEvent WHERE status IN (:statusValues) AND remindedTimestamp > :fromTimestamp ORDER BY remindedTimestamp")
    List<ReminderEvent> getLiveReminderEvents(long fromTimestamp, List<ReminderEvent.ReminderStatus> statusValues);

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

    @Query("DELETE FROM Reminder")
    void deleteReminders();

    @Query("DELETE FROM Medicine")
    void deleteMedicines();
}
