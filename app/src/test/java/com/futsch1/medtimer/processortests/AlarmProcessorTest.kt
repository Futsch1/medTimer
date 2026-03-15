package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.preferences.MedTimerSettings
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
        reminderContext.medTimerSettings = MedTimerSettings(exactReminders = true)
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        processedNotificationData.remindInstant = processedNotificationData.remindInstant.plusSeconds(10)

        runBlocking {
            AlarmProcessor(reminderContext.mock).setAlarmForReminderNotification(
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