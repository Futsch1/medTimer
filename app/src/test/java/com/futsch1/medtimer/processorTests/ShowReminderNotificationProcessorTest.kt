package com.futsch1.medtimer.processorTests

import com.futsch1.medtimer.reminders.ShowReminderNotificationProcessor
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
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
}