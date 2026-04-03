package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.di.DatabaseModule
import com.futsch1.medtimer.di.DatastoreModule
import com.futsch1.medtimer.di.TimeAccessModule
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationPart
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import kotlin.test.assertEquals

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@UninstallModules(
    DatabaseModule::class,
    DatastoreModule::class,
    TimeAccessModule::class
)
class NotificationProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val testReminderContext = TestReminderContext()

    @BindValue
    val boundAlarmManager: AlarmManager = testReminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = testReminderContext.notificationManagerFake.mock

    @BindValue
    val boundMedicineRepository: com.futsch1.medtimer.database.MedicineRepository = testReminderContext.medicineRepositoryFake.mock

    @BindValue
    val boundPreferencesDataSource: com.futsch1.medtimer.preferences.PreferencesDataSource = testReminderContext.preferencesDataSourceMock

    @BindValue
    val boundPersistentDataDataSource: com.futsch1.medtimer.preferences.PersistentDataDataSource = testReminderContext.persistentDataDataSourceMock

    @BindValue
    val boundTimeAccess: com.futsch1.medtimer.reminders.TimeAccess = object : com.futsch1.medtimer.reminders.TimeAccess {
        override fun systemZone(): java.time.ZoneId = java.time.ZoneId.of("UTC")
        override fun localDate(): java.time.LocalDate = testReminderContext.localDate
        override fun now(): Instant = testReminderContext.instant
    }

    @BindValue
    val boundMedicineRoomDatabase: com.futsch1.medtimer.database.MedicineRoomDatabase = mock()

    @BindValue
    val boundMedicineDao: com.futsch1.medtimer.database.MedicineDao = mock()

    @BindValue
    @com.futsch1.medtimer.di.DefaultPreferences
    val boundDefaultSharedPreferences: android.content.SharedPreferences = mock()

    @BindValue
    @com.futsch1.medtimer.di.MedTimerPreferencess
    val boundMedTimerSharedPreferences: android.content.SharedPreferences = mock()

    @BindValue
    val boundReminderNotificationFactory: ReminderNotificationFactory = object : ReminderNotificationFactory(mock(), mock(), mock()) {
        override suspend fun create(reminderNotificationData: ReminderNotificationData): ReminderNotification? {
            if (!reminderNotificationData.valid) return null

            val parts = mutableListOf<ReminderNotificationPart>()
            for (i in reminderNotificationData.reminderIds.indices) {
                val reminder = testReminderContext.medicineRepositoryFake.mock.getReminder(reminderNotificationData.reminderIds[i])
                    ?: return null
                val medicine = testReminderContext.medicineRepositoryFake.mock.getMedicine(reminder.medicineRelId)
                    ?: return null
                val event = testReminderContext.medicineRepositoryFake.mock.getReminderEvent(reminderNotificationData.reminderEventIds[i])
                    ?: return null
                parts.add(ReminderNotificationPart(reminder, event, medicine))
            }
            return ReminderNotification(parts, reminderNotificationData)
        }
    }

    @Inject
    lateinit var notificationProcessor: NotificationProcessor

    @Test
    fun singleTaken() {
        testReminderContext.instant = Instant.ofEpochSecond(10)
        val reminderNotificationData = fillWithOneReminder(testReminderContext)
        val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotificationData)
        testReminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        testReminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(1))

        runBlocking {
            notificationProcessor.processReminderEventsInNotification(
                processedNotificationData,
                ReminderEvent.ReminderStatus.TAKEN
            )
        }

        // Reminder marked as taken
        assertEquals(ReminderEvent.ReminderStatus.TAKEN, testReminderContext.medicineRepositoryFake.reminderEvents[0].status)
        // Processed time stamp set
        assertEquals(10, testReminderContext.medicineRepositoryFake.reminderEvents[0].processedTimestamp)
        // Notification removed
        verify(testReminderContext.notificationManagerFake.mock, times(1)).cancel(1)
        // No new notification raised
        verify(testReminderContext.notificationManagerFake.mock, never()).notify(anyInt(), any())
    }

    @Test
    fun removeSingle() {
        testReminderContext.instant = Instant.ofEpochSecond(10)
        testReminderContext.notificationId = 2
        fillWithTwoReminders(testReminderContext)
        testReminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        testReminderContext.medicineRepositoryFake.reminderEvents[1].notificationId = 1
        testReminderContext.notificationManagerFake.add(1, intArrayOf(1, 2), intArrayOf(1, 2))

        val processedNotificationData = ProcessedNotificationData(listOf(2))

        runBlocking {
            notificationProcessor.processReminderEventsInNotification(
                processedNotificationData,
                ReminderEvent.ReminderStatus.SKIPPED
            )
        }
        // Reminder marked as taken
        assertEquals(ReminderEvent.ReminderStatus.SKIPPED, testReminderContext.medicineRepositoryFake.reminderEvents[1].status)
        // Processed time stamp set
        assertEquals(10, testReminderContext.medicineRepositoryFake.reminderEvents[1].processedTimestamp)
        // Notification not removed
        verify(testReminderContext.notificationManagerFake.mock, never()).cancel(1)
        // But notification updated
        verify(testReminderContext.notificationManagerFake.mock, times(1)).notify(eq(1), any())
        // First reminder still raised
        assertEquals(ReminderEvent.ReminderStatus.RAISED, testReminderContext.medicineRepositoryFake.reminderEvents[0].status)
    }
}
