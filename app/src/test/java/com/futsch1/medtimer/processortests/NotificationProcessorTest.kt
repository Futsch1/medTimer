package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.time.Instant

class NotificationProcessorTest {
    @Test
    fun singleTaken() {
        val reminderContext = TestReminderContext()
        reminderContext.instant = Instant.ofEpochSecond(10)
        val reminderNotificationData = fillWithOneReminder(reminderContext)
        val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotificationData)
        reminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        reminderContext.notificationManagerFake.add(1, reminderEventIds = intArrayOf(1))

        NotificationProcessor(reminderContext.mock).processReminderEventsInNotification(processedNotificationData, ReminderEvent.ReminderStatus.TAKEN)

        // Reminder marked as taken
        assertEquals(ReminderEvent.ReminderStatus.TAKEN, reminderContext.medicineRepositoryFake.reminderEvents[0].status)
        // Processed time stamp set
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[0].processedTimestamp)
        // Notification removed
        verify(reminderContext.notificationManagerFake.mock, times(1)).cancel(1)
        // No new notification raised
        verify(reminderContext.notificationManagerFake.mock, never()).notify(anyInt(), any())
    }

    @Test
    fun removeSingle() {
        val reminderContext = TestReminderContext()
        reminderContext.instant = Instant.ofEpochSecond(10)
        reminderContext.notificationId = 2
        fillWithTwoReminders(reminderContext)
        reminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        reminderContext.medicineRepositoryFake.reminderEvents[1].notificationId = 1
        reminderContext.notificationManagerFake.add(1, intArrayOf(1, 2), intArrayOf(1, 2))

        val processedNotificationData = ProcessedNotificationData(listOf(2))

        NotificationProcessor(reminderContext.mock).processReminderEventsInNotification(processedNotificationData, ReminderEvent.ReminderStatus.SKIPPED)
        // Reminder marked as taken
        assertEquals(ReminderEvent.ReminderStatus.SKIPPED, reminderContext.medicineRepositoryFake.reminderEvents[1].status)
        // Processed time stamp set
        assertEquals(10, reminderContext.medicineRepositoryFake.reminderEvents[1].processedTimestamp)
        // Notification not removed
        verify(reminderContext.notificationManagerFake.mock, never()).cancel(1)
        // But notification updated
        verify(reminderContext.notificationManagerFake.mock, times(1)).notify(eq(1), any())
        // First reminder still raised
        assertEquals(ReminderEvent.ReminderStatus.RAISED, reminderContext.medicineRepositoryFake.reminderEvents[0].status)
    }
}