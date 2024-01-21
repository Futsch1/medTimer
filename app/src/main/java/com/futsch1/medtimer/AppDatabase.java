package com.futsch1.medtimer;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineDao;
import com.futsch1.medtimer.database.ReminderEntity;

@Database(entities = {Medicine.class, ReminderEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MedicineDao medicineDao();
}
