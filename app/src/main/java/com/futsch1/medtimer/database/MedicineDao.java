package com.futsch1.medtimer.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM Medicine")
    LiveData<List<MedicineWithReminders>> getMedicines();

    @Insert
    void insertMedicine(Medicine medicineEntities);

    @Insert
    void insertReminder(Reminder medicines);

    @Delete
    void deleteMedicine(Medicine medicine);

    @Delete
    void deleteReminder(Reminder reminder);
}
