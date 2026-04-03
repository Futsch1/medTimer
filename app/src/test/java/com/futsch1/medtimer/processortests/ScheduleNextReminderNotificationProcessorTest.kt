package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import com.futsch1.medtimer.reminders.ScheduleNextReminderNotificationProcessor
import com.futsch1.medtimer.schedulertests.TestHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@dagger.hilt.android.testing.UninstallModules(
    com.futsch1.medtimer.di.DatabaseModule::class,
    com.futsch1.medtimer.di.DatastoreModule::class,
    com.futsch1.medtimer.di.TimeAccessModule::class
)
class ScheduleNextReminderNotificationProcessorTest {
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
    lateinit var scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor

    @Test
    fun noReminder() {
        runBlocking {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }

        verify(reminderContext.alarmManagerMock, times(1)).cancel(any<PendingIntent>())
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), anyLong(), any())
    }

    @Test
    fun scheduleReminder() {
        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1))

        runBlocking {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()
        }

        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(600 * 60 * 1000L), any())
    }
}