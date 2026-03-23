package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.reminders.AlarmProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class AlarmProcessorTest {
    @Test
    fun exactAlarms() {
        val reminderContext = TestReminderContext()
        reminderContext.userPreferences = UserPreferences.default().copy(exactReminders = true)
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        processedNotificationData.remindInstant = processedNotificationData.remindInstant.plusSeconds(10)

        runBlocking {
            AlarmProcessor(reminderContext.mock, reminderContext.alarmManagerMock).setAlarmForReminderNotification(
                processedNotificationData
            )
        }

        verify(reminderContext.alarmManagerMock, times(1)).setExactAndAllowWhileIdle(
            anyInt(),
            anyLong(),
            any()
        )
    }


}