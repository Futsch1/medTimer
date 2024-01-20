package com.futsch1.medtimer.logic;

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
    List<Medicine> getMedicines();

    @Insert
    void insertMedicines(Medicine... medicines);

    @Insert
    void insertReminders(Reminder... medicines);

    @Delete
    void deleteMedicine(Medicine medicine);

    @Delete
    void deleteReminder(Reminder reminder);
}
