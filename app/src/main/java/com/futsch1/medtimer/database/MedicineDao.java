package com.futsch1.medtimer.database;

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
    List<MedicineWithReminders> getMedicines();

    @Insert
    void insertMedicines(Medicine... medicineEntities);

    @Insert
    void insertReminders(ReminderEntity... medicines);

    @Delete
    void deleteMedicine(Medicine medicine);

    @Delete
    void deleteReminder(ReminderEntity reminder);
}
