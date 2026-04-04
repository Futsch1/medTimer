package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.futsch1.medtimer.database.DatabaseManager
import com.futsch1.medtimer.database.MedicineDao
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.MedicineRoomDatabase
import com.futsch1.medtimer.database.ReminderDao
import com.futsch1.medtimer.database.ReminderEventDao
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.database.TagDao
import com.futsch1.medtimer.database.TagRepository
import com.futsch1.medtimer.di.DatabaseModule
import com.futsch1.medtimer.di.DatastoreModule
import com.futsch1.medtimer.di.TimeAccessModule
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.ShowReminderNotificationProcessor
import com.futsch1.medtimer.reminders.TimeAccess
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class ShowReminderNotificationProcessorTest {
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
    val boundMedicineRepository: MedicineRepository = reminderContext.repositoryFakes.medicineRepositoryMock

    @BindValue
    val boundReminderRepository: ReminderRepository = reminderContext.repositoryFakes.reminderRepositoryMock

    @BindValue
    val boundReminderEventRepository: ReminderEventRepository = reminderContext.repositoryFakes.reminderEventRepositoryMock

    @BindValue
    val boundPreferencesDataSource: PreferencesDataSource = reminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: PersistentDataDataSource = reminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: TimeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = ZoneId.of("UTC")
        override fun localDate(): LocalDate = reminderContext.localDate
        override fun now(): Instant = reminderContext.instant
    }

    @BindValue
    val boundMedicineRoomDatabase: MedicineRoomDatabase = mock()

    @BindValue
    val boundMedicineDao: MedicineDao = mock()

    @BindValue
    val boundReminderDao: ReminderDao = mock()

    @BindValue
    val boundReminderEventDao: ReminderEventDao = mock()

    @BindValue
    val boundTagDao: TagDao = mock()

    @BindValue
    val boundTagRepository: TagRepository = mock()

    @BindValue
    val boundDatabaseManager: DatabaseManager = mock()

    @BindValue
    @com.futsch1.medtimer.di.DefaultPreferences
    val boundDefaultSharedPreferences: SharedPreferences = mock()

    @BindValue
    @com.futsch1.medtimer.di.MedTimerPreferencess
    val boundMedTimerSharedPreferences: SharedPreferences = mock()

    @Inject
    lateinit var showReminderNotificationProcessor: ShowReminderNotificationProcessor

    @Test
    fun reminderNotificationNotActive() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)

        runBlocking {
            showReminderNotificationProcessor.showReminder(
                reminderNotificationData
            )
        }

        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And the one for tomorrow
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }

    @Test
    fun reminderNotificationActive() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)
        reminderNotificationData.notificationId = 1
        reminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(1, 2), remindTimestamp = 10)

        runBlocking {
            showReminderNotificationProcessor.showReminder(
                reminderNotificationData
            )
        }

        // Never cancel a notification
        verify(reminderContext.notificationManagerFake.mock, never()).cancel(anyInt())
        // Never raise a new one
        verify(reminderContext.notificationManagerFake.mock, never()).notify(anyInt(), any())
        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And the one for tomorrow
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }

    @Test
    fun reminderNotificationPartlyActive() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderContext.repositoryFakes.reminderEvents[0].notificationId = 1
        reminderContext.repositoryFakes.reminderEvents[1].notificationId = 1
        reminderContext.persistentData = reminderContext.persistentData.copy(notificationId = 2)
        reminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(2))

        runBlocking {
            showReminderNotificationProcessor.showReminder(
                reminderNotificationData
            )
        }

        // Send broadcast (one for the reminder and one for updating the widget)
        val broadcastIntents = shadowOf(ApplicationProvider.getApplicationContext<Application>()).broadcastIntents
        assertEquals(2, broadcastIntents.size)
        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And two times for tomorrow (twice is actually unnecessary, but to reduce complexity, scheduling is called more often)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }

    @Test
    fun rescheduleReminderEvent() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderContext.repositoryFakes.reminderEvents[0].notificationId = 1
        reminderContext.repositoryFakes.reminderEvents[1].notificationId = 1

        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)
        reminderNotificationData.notificationId = 1
        reminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(1, 2))

        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)

        runBlocking {
            showReminderNotificationProcessor.showReminder(
                reminderNotificationData
            )
        }

        // Cancel the notification
        verify(reminderContext.notificationManagerFake.mock, times(1)).cancel(1)
        // The rescheduled reminder
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
    }
}
