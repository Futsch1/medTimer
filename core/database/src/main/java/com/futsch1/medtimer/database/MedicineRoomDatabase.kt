package com.futsch1.medtimer.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration16To17
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration1To2
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration20To21
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration21To22
import com.futsch1.medtimer.database.MedicineRoomDatabase.AutoMigration5To6
import com.futsch1.medtimer.database.dao.MedicineDao
import com.futsch1.medtimer.database.dao.ReminderDao
import com.futsch1.medtimer.database.dao.ReminderEventDao
import com.futsch1.medtimer.database.dao.TagDao

@Database(
    entities = [MedicineEntity::class, ReminderEntity::class, ReminderEventEntity::class, TagEntity::class, MedicineToTagEntity::class],
    version = 22,
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
    ), AutoMigration(from = 21, to = 22, spec = AutoMigration21To22::class)],
)
@TypeConverters(Converters::class)
abstract class MedicineRoomDatabase : RoomDatabase() {
    val version: Int
        get() = openHelper.readableDatabase.version

    abstract fun medicineDao(): MedicineDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderEventDao(): ReminderEventDao
    abstract fun tagDao(): TagDao

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
}
