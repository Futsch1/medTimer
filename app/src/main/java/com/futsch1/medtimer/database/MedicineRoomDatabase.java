package com.futsch1.medtimer.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.DeleteColumn;
import androidx.room.RenameColumn;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.AutoMigrationSpec;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Database(
        entities = {Medicine.class, Reminder.class, ReminderEvent.class, Tag.class, MedicineToTag.class, StockEvent.class},
        version = 22,
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
                @AutoMigration(from = 10, to = 11),
                @AutoMigration(from = 11, to = 12),
                @AutoMigration(from = 12, to = 13),
                @AutoMigration(from = 13, to = 14),
                @AutoMigration(from = 14, to = 15),
                @AutoMigration(from = 15, to = 16),
                @AutoMigration(from = 16, to = 17, spec = MedicineRoomDatabase.AutoMigration16To17.class),
                @AutoMigration(from = 17, to = 18),
                @AutoMigration(from = 18, to = 19),
                @AutoMigration(from = 19, to = 20),
                @AutoMigration(from = 20, to = 21, spec = MedicineRoomDatabase.AutoMigration20To21.class),
                @AutoMigration(from = 21, to = 22, spec = MedicineRoomDatabase.AutoMigration21To22.class)
        }
)
@TypeConverters({Converters.class})
@SuppressWarnings("java:S6548")
public abstract class MedicineRoomDatabase extends RoomDatabase {
    private static final int NUMBER_OF_THREADS = 1;
    static final ExecutorService databaseWriteExecutor =
            new IdlingThreadPoolExecutor(
                    "DatabaseWriteExecutor", NUMBER_OF_THREADS, NUMBER_OF_THREADS, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                    new ThreadFactory() {
                        private int count = 1;

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "DatabaseWrite-" + count++);
                        }
                    });
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

    public abstract StockDao stockDao();

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

    static class AutoMigration16To17 implements AutoMigrationSpec {
        @Override
        public void onPostMigrate(@NonNull SupportSQLiteDatabase db) {
            AutoMigrationSpec.super.onPostMigrate(db);

            db.execSQL("""
                        CREATE TEMP TABLE IF NOT EXISTS temp_table AS
                             SELECT
                                 m.*,
                                 (SELECT COUNT(*) FROM Medicine AS m2 WHERE m2.rowid <= m.rowid) AS rn
                             FROM Medicine AS m;
                    """);

            db.execSQL("""
                        UPDATE Medicine
                        SET sortOrder = (
                           SELECT CAST(rn AS DOUBLE)
                           FROM temp_table
                           WHERE temp_table.medicineId = Medicine.medicineId
                       );
                    """);

            db.execSQL("DROP TABLE temp_table;");
        }
    }


    static class AutoMigration20To21 implements AutoMigrationSpec {
        @Override
        public void onPostMigrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ReminderEvent_remindedTimestamp` ON `ReminderEvent` (`remindedTimestamp`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ReminderEvent_reminderId` ON `ReminderEvent` (`reminderId`)");
        }
    }

    @DeleteColumn(tableName = "Medicine", columnName = "outOfStockReminderThreshold")
    @DeleteColumn(tableName = "Medicine", columnName = "outOfStockReminder")
    static class AutoMigration21To22 implements AutoMigrationSpec {

    }
}
