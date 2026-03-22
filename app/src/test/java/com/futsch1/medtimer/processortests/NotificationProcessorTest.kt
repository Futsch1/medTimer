package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
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
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import kotlin.test.assertEquals

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationProcessorTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    val testReminderContext = TestReminderContext()

    @BindValue
    val reminderContext: ReminderContext = testReminderContext.mock

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