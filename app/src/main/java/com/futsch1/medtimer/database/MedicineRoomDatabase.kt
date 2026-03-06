package com.futsch1.medtimer.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration16To17
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration1To2
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration20To21
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration21To22
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration22To23
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration5To6
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

@Database(
    entities = [Medicine::class, Reminder::class, ReminderEvent::class, Tag::class, MedicineToTag::class],
    version = 23,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = AutoMigration1To2::class), AutoMigration(from = 2, to = 3), AutoMigration(
        from = 3,
        to = 4
    ), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6, spec = AutoMigration5To6::class), AutoMigration(
        from = 6,
        to = 7
    ), AutoMigration(from = 7, to = 8), AutoMigration(from = 8, to = 9), AutoMigration(from = 9, to = 10), AutoMigration(
        from = 10,
        to = 11
    ), AutoMigration(from = 11, to = 12), AutoMigration(from = 12, to = 13), AutoMigration(from = 13, to = 14), AutoMigration(
        from = 14,
        to = 15
    ), AutoMigration(from = 15, to = 16), AutoMigration(from = 16, to = 17, spec = AutoMigration16To17::class), AutoMigration(
        from = 17,
        to = 18
    ), AutoMigration(from = 18, to = 19), AutoMigration(from = 19, to = 20), AutoMigration(
        from = 20,
        to = 21,
        spec = AutoMigration20To21::class
    ), AutoMigration(from = 21, to = 22, spec = AutoMigration21To22::class), AutoMigration(from = 22, to = 23, spec = AutoMigration22To23::class)]
)
@TypeConverters(Converters::class)
abstract class MedicineRoomDatabase : RoomDatabase() {
    val version: Int
        get() = openHelper.readableDatabase.version

    abstract fun medicineDao(): MedicineDao

    @RenameColumn(fromColumnName = "raisedTimestamp", toColumnName = "remindedTimestamp", tableName = "ReminderEvent")
    internal class AutoMigration1To2 : AutoMigrationSpec

    @RenameColumn(fromColumnName = "daysBetweenReminders", toColumnName = "pauseDays", tableName = "Reminder")
    internal class AutoMigration5To6 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            db.execSQL("UPDATE Reminder SET pauseDays = pauseDays - 1")
        }
    }

    internal class AutoMigration16To17 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)

            db.execSQL(
                """
                        CREATE TEMP TABLE IF NOT EXISTS temp_table AS
                             SELECT
                                 m.*,
                                 (SELECT COUNT(*) FROM Medicine AS m2 WHERE m2.rowid <= m.rowid) AS rn
                             FROM Medicine AS m;
                    
                    """.trimIndent()
            )

            db.execSQL(
                """
                        UPDATE Medicine
                        SET sortOrder = (
                           SELECT CAST(rn AS DOUBLE)
                           FROM temp_table
                           WHERE temp_table.medicineId = Medicine.medicineId
                       );
                    
                    """.trimIndent()
            )

            db.execSQL("DROP TABLE temp_table;")
        }
    }


    internal class AutoMigration20To21 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ReminderEvent_remindedTimestamp` ON `ReminderEvent` (`remindedTimestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ReminderEvent_reminderId` ON `ReminderEvent` (`reminderId`)")
        }
    }

    @DeleteColumn(tableName = "Medicine", columnName = "outOfStockReminderThreshold")
    @DeleteColumn(tableName = "Medicine", columnName = "outOfStockReminder")
    internal class AutoMigration21To22 : AutoMigrationSpec

    internal class AutoMigration22To23 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE Medicine SET medicineName = '' WHERE medicineName IS NULL")
            db.execSQL("UPDATE Medicine SET refillSizes = '[]' WHERE refillSizes IS NULL")
            db.execSQL("UPDATE Medicine SET unit = '' WHERE unit IS NULL")
        }
    }

    companion object {
        private const val NUMBER_OF_THREADS = 1
        val databaseWriteExecutor: ExecutorService = IdlingThreadPoolExecutor(
            "DatabaseWriteExecutor", NUMBER_OF_THREADS, NUMBER_OF_THREADS, 100, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(),
            object : ThreadFactory {
                private var count = 1

                override fun newThread(r: Runnable?): Thread {
                    return Thread(r, "DatabaseWrite-" + count++)
                }
            })

        // marking the instance as volatile to ensure atomic access to the variable
        @Volatile
        private var instance: MedicineRoomDatabase? = null

        fun getDatabase(context: Context): MedicineRoomDatabase {
            if (instance == null) {
                synchronized(MedicineRoomDatabase::class.java) {
                    if (instance == null) {
                        instance = databaseBuilder(
                            context.applicationContext,
                            MedicineRoomDatabase::class.java, "medTimer"
                        )
                            .build()
                    }
                }
            }
            return instance!!
        }
    }
}
