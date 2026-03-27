package com.futsch1.medtimer.processortests

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import com.futsch1.medtimer.reminders.ReminderContext
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
class ScheduleNextReminderNotificationProcessorTest {
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