package com.futsch1.medtimer.logic;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MedicineDao {
    @Query("SELECT * FROM Medicine")
    List<Medicine> getAll();

    @Insert
    void insertAll(Medicine... medicines);

    @Delete
    void delete(Medicine medicine);
}
