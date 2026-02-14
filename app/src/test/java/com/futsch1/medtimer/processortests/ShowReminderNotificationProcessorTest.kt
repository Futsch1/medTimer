package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.reminders.ShowReminderNotificationProcessor
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ShowReminderNotificationProcessorTest {
    @Test
    fun reminderNotificationNotActive() {
        val reminderContext = TestReminderContext()
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)

        ShowReminderNotificationProcessor(reminderContext.mock).showReminder(reminderNotificationData)

        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And the one for tomorrow
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }

    @Test
    fun reminderNotificationActive() {
        val reminderContext = TestReminderContext()
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderNotificationData.remindInstant = reminderNotificationData.remindInstant.plusSeconds(10)
        reminderNotificationData.notificationId = 1
        reminderContext.notificationManagerFake.add(1, intArrayOf(1, 2))

        ShowReminderNotificationProcessor(reminderContext.mock).showReminder(reminderNotificationData)

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
        val reminderContext = TestReminderContext()
        val reminderNotificationData = fillWithTwoReminders(reminderContext)
        reminderContext.medicineRepositoryFake.reminderEvents[0].notificationId = 1
        reminderContext.medicineRepositoryFake.reminderEvents[1].notificationId = 1
        reminderContext.notificationId = 2
        reminderContext.notificationManagerFake.add(1, intArrayOf(2))

        ShowReminderNotificationProcessor(reminderContext.mock).showReminder(reminderNotificationData)

        // Cancel the notification
        verify(reminderContext.notificationManagerFake.mock, times(1)).cancel(1)
        // Raise a new one
        verify(reminderContext.notificationManagerFake.mock, times(1)).notify(eq(2), any())
        // The actually requested reminder
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
        // And two times for tomorrow (twice is actually unnecessary, but to reduce complexity, scheduling is called more often)
        verify(reminderContext.alarmManagerMock, times(2)).setAndAllowWhileIdle(anyInt(), eq(24 * 60 * 60 * 1000L + 10 * 60 * 60 * 1000L), any())
    }
}