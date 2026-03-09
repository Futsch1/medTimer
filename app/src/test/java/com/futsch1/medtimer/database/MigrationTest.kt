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
    fun testMigrate22To23() {
        // Create version 22 of the database.
        var db = helper.createDatabase(testDb, 22)

        // Insert some data with NULL values
        db.execSQL("INSERT INTO Medicine (medicineName, color, useColor, notificationImportance, iconId, amount, refillSizes, unit, sortOrder, notes, showNotificationAsAlarm, productionDate, expirationDate) VALUES (NULL, 0, 0, 3, 0, 0.0, NULL, NULL, 1.0, '', 0, 0, 0)")
        db.execSQL("INSERT INTO Tag (name, tagId) VALUES (NULL, 1)")

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 23 and validate
        db = helper.runMigrationsAndValidate(testDb, 23, true, MedicineRoomDatabase.MIGRATION_22_23)

        // Verify data was migrated properly
        var cursor = db.query("SELECT medicineName, refillSizes, unit FROM Medicine")
        cursor.moveToFirst()
        assertEquals("", cursor.getString(0))
        assertEquals("[]", cursor.getString(1))
        assertEquals("", cursor.getString(2))
        cursor.close()

        cursor = db.query("SELECT name FROM Tag")
        cursor.moveToFirst()
        assertEquals("", cursor.getString(0))
        cursor.close()

        db.close()
    }
}
