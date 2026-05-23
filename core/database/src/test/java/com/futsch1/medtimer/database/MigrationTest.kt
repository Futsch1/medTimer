package com.futsch1.medtimer.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    private lateinit var helper: MigrationTestHelper

    @Before
    fun setUp() {
        helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MedicineRoomDatabase::class.java
        )
    }

    @Test
    @Throws(IOException::class)
    fun testMigrate1To2() {
        // Create version 1 of the database and insert a ReminderEvent using the old raisedTimestamp column.
        var db = helper.createDatabase(testDb, 1)
        db.execSQL(
            "INSERT INTO ReminderEvent (medicineName, amount, status, raisedTimestamp, processedTimestamp, reminderId) VALUES ('Aspirin', '1.0', 'OPEN', 1700000000, 0, 42)"
        )
        db.close()

        // Run migration to version 2 (raisedTimestamp renamed to remindedTimestamp).
        db = helper.runMigrationsAndValidate(testDb, 2, true)

        val cursor = db.query("SELECT remindedTimestamp FROM ReminderEvent")
        cursor.moveToFirst()
        assertEquals(1700000000L, cursor.getLong(0))
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigrate5To6() {
        // Create version 5 of the database and insert a Reminder with daysBetweenReminders = 3.
        var db = helper.createDatabase(testDb, 5)
        db.execSQL(
            "INSERT INTO Reminder (medicineRelId, timeInMinutes, createdTimestamp, daysBetweenReminders, instructions, amount) VALUES (1, 480, 0, 3, '', '1.0')"
        )
        db.close()

        // Run migration to version 6: column renamed to pauseDays and decremented by 1.
        db = helper.runMigrationsAndValidate(testDb, 6, true)

        val cursor = db.query("SELECT pauseDays FROM Reminder")
        cursor.moveToFirst()
        assertEquals(2, cursor.getInt(0))
        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigrate16To17() {
        // Create version 16 of the database and insert two Medicine rows (no sortOrder column yet).
        var db = helper.createDatabase(testDb, 16)
        db.execSQL(
            "INSERT INTO Medicine (medicineName, color, useColor, notificationImportance, iconId, outOfStockReminder, amount, outOfStockReminderThreshold, refillSizes, unit) VALUES ('Alpha', 0, 0, 3, 0, 'OFF', 0.0, 0.0, '[]', '')"
        )
        db.execSQL(
            "INSERT INTO Medicine (medicineName, color, useColor, notificationImportance, iconId, outOfStockReminder, amount, outOfStockReminderThreshold, refillSizes, unit) VALUES ('Beta', 0, 0, 3, 0, 'OFF', 0.0, 0.0, '[]', '')"
        )
        db.close()

        // Run migration to version 17: sortOrder populated with row numbers.
        db = helper.runMigrationsAndValidate(testDb, 17, true)

        val cursor = db.query("SELECT medicineName, sortOrder FROM Medicine ORDER BY sortOrder")
        cursor.moveToFirst()
        assertEquals("Alpha", cursor.getString(0))
        assertEquals(1.0, cursor.getDouble(1), 0.001)
        cursor.moveToNext()
        assertEquals("Beta", cursor.getString(0))
        assertEquals(2.0, cursor.getDouble(1), 0.001)
        cursor.close()
        db.close()
    }
}
