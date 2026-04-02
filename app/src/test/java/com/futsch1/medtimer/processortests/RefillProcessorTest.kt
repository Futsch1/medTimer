package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.reminders.RefillProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.schedulertests.TestHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import kotlin.test.assertEquals

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@dagger.hilt.android.testing.UninstallModules(
    com.futsch1.medtimer.di.DatabaseModule::class,
    com.futsch1.medtimer.di.DatastoreModule::class,
    com.futsch1.medtimer.di.TimeAccessModule::class
)
class RefillProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val reminderContext = TestReminderContext()

    @BindValue
    val boundAlarmManager: AlarmManager = reminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = reminderContext.notificationManagerFake.mock

    @BindValue
    val boundMedicineRepository: com.futsch1.medtimer.database.MedicineRepository = reminderContext.medicineRepositoryFake.mock

    @BindValue
    val boundPreferencesDataSource: com.futsch1.medtimer.preferences.PreferencesDataSource = reminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: com.futsch1.medtimer.preferences.PersistentDataDataSource = reminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: com.futsch1.medtimer.reminders.TimeAccess = object : com.futsch1.medtimer.reminders.TimeAccess {
        override fun systemZone(): java.time.ZoneId = java.time.ZoneId.of("UTC")
        override fun localDate(): java.time.LocalDate = reminderContext.localDate
        override fun now(): java.time.Instant = reminderContext.instant
    }

    @BindValue
    val boundMedicineRoomDatabase: com.futsch1.medtimer.database.MedicineRoomDatabase = org.mockito.Mockito.mock()

    @BindValue
    val boundMedicineDao: com.futsch1.medtimer.database.MedicineDao = org.mockito.Mockito.mock()

    @BindValue
    @com.futsch1.medtimer.di.DefaultPreferences
    val boundDefaultSharedPreferences: android.content.SharedPreferences = org.mockito.Mockito.mock()

    @BindValue
    @com.futsch1.medtimer.di.MedTimerPreferencess
    val boundMedTimerSharedPreferences: android.content.SharedPreferences = org.mockito.Mockito.mock()

    @Inject
    lateinit var refillProcessor: RefillProcessor

    @Test
    fun directRefill() {
        reminderContext.instant = Instant.ofEpochSecond(10)

        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0

        runBlocking {
            refillProcessor.processRefill(1)
        }

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[0].processedTimestamp)
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[0].remindedTimestamp)
        assertEquals("100 ➡ 110", reminderContext.medicineRepositoryFake.reminderEvents[0].amount)
        assertEquals(ReminderEntity.ReminderType.REFILL, reminderContext.medicineRepositoryFake.reminderEvents[0].reminderType)
    }

    @Test
    fun refillViaEvent() {
        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 0, 1))
        reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1))

        runBlocking {
            refillProcessor.processRefill(ProcessedNotificationData(listOf(1)))
        }

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals(ReminderEventEntity.ReminderStatus.ACKNOWLEDGED, reminderContext.medicineRepositoryFake.reminderEvents[0].status)
    }
}