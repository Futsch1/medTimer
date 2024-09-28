package com.futsch1.medtimer.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RenameColumn;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.AutoMigrationSpec;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {Medicine.class, Reminder.class, ReminderEvent.class},
        version = 10,
        autoMigrations = {
                @AutoMigration(from = 1, to = 2, spec = MedicineRoomDatabase.AutoMigration1To2.class),
                @AutoMigration(from = 2, to = 3),
                @AutoMigration(from = 3, to = 4),
                @AutoMigration(from = 4, to = 5),
                @AutoMigration(from = 5, to = 6, spec = MedicineRoomDatabase.AutoMigration5To6.class),
                @AutoMigration(from = 6, to = 7),
                @AutoMigration(from = 7, to = 8),
                @AutoMigration(from = 8, to = 9),
                @AutoMigration(from = 9, to = 10),
        }
)
@TypeConverters({Converters.class})
@SuppressWarnings("java:S6548")
public abstract class MedicineRoomDatabase extends RoomDatabase {
    private static final int NUMBER_OF_THREADS = 1;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    // marking the instance as volatile to ensure atomic access to the variable
    @SuppressWarnings("java:S3077")
    private static volatile MedicineRoomDatabase instance;

    static MedicineRoomDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (MedicineRoomDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    MedicineRoomDatabase.class, "medTimer")
                            .build();
                }
            }
        }
        return instance;
    }

    public int getVersion() {
        return getOpenHelper().getReadableDatabase().getVersion();
    }

    public abstract MedicineDao medicineDao();

    @RenameColumn(fromColumnName = "raisedTimestamp", toColumnName = "remindedTimestamp", tableName = "ReminderEvent")
    static class AutoMigration1To2 implements AutoMigrationSpec {
    }

    @RenameColumn(fromColumnName = "daysBetweenReminders", toColumnName = "pauseDays", tableName = "Reminder")
    static class AutoMigration5To6 implements AutoMigrationSpec {
        @Override
        public void onPostMigrate(@NonNull SupportSQLiteDatabase db) {
            AutoMigrationSpec.super.onPostMigrate(db);
            db.execSQL("UPDATE Reminder SET pauseDays = pauseDays - 1");
        }
    }
}
