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
    LiveData<List<MedicineWithReminders>> getMedicines();

    @Query("SELECT * FROM Reminder WHERE medicineRelId= :medicineId")
    LiveData<List<Reminder>> getReminders(int medicineId);

    @Query("SELECT * FROM ReminderEvent")
    LiveData<List<ReminderEvent>> getReminderEvents();

    @Insert
    void insertMedicine(Medicine medicine);

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
}
