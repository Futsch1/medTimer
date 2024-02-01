package com.futsch1.medtimer.database;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RenameColumn;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.AutoMigrationSpec;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {Medicine.class, Reminder.class, ReminderEvent.class},
        version = 2,
        autoMigrations = {
                @AutoMigration(from = 1, to = 2, spec = MedicineRoomDatabase.AutoMigration1To2.class),
        }
)
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
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract MedicineDao medicineDao();

    @RenameColumn(fromColumnName = "raisedTimestamp", toColumnName = "remindedTimestamp", tableName = "ReminderEvent")
    static class AutoMigration1To2 implements AutoMigrationSpec {
    }
}
