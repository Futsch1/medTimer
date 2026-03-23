package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.media.AudioManager
import com.futsch1.medtimer.di.SystemServicesModule
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.ShowReminderNotificationProcessor
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(SystemServicesModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShowReminderNotificationProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val reminderContext = TestReminderContext()

    @BindValue
    val boundReminderContext: ReminderContext = reminderContext.mock

    @BindValue
    val boundAlarmManager: AlarmManager = reminderContext.alarmManagerMock

    @BindValue
    val boundNotificationManager: NotificationManager = reminderContext.notificationManagerFake.mock

    @BindValue
    val boundAudioManager: AudioManager = mock()

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
        reminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        reminderContext.medicineRepositoryFake.reminderEvents[1].notificationId = 1
        reminderContext.persistentData = reminderContext.persistentData.copy(notificationId = 2)
        reminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(2))

        runBlocking {
            showReminderNotificationProcessor.showReminder(
                reminderNotificationData
            )
        }

        // Send broadcast (one for the reminder and one for updating the widget)
        verify(reminderContext.mock, times(2)).sendBroadcast(anyNotNull(), anyNotNull())
        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And two times for tomorrow (twice is actually unnecessary, but to reduce complexity, scheduling is called more often)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }

    @Test
    fun rescheduleReminderEvent() {
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        reminderContext.medicineRepositoryFake.reminderEvents[1].notificationId = 1

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

    private fun <T> anyNotNull(): T = any()
}
