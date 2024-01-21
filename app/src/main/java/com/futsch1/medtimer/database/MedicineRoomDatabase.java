package com.futsch1.medtimer.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Medicine.class, Reminder.class}, version = 1)
public abstract class MedicineRoomDatabase extends RoomDatabase {
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    // marking the instance as volatile to ensure atomic access to the variable
    private static volatile MedicineRoomDatabase INSTANCE;

    static MedicineRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MedicineRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MedicineRoomDatabase.class, "medTimer")
//                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract MedicineDao medicineDao();
}
