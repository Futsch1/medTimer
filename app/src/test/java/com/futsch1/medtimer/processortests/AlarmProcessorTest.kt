package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.AlarmProcessor
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class AlarmProcessorTest {
    @Test
    fun exactAlarms() {
        val reminderContext = TestReminderContext()
        val processedNotificationData = fillWithTwoReminders(reminderContext)
        reminderContext.boolPreferencesMap[PreferencesNames.EXACT_REMINDERS] = true
        processedNotificationData.remindInstant = processedNotificationData.remindInstant.plusSeconds(10)

        AlarmProcessor(reminderContext.mock).setAlarmForReminderNotification(processedNotificationData)

        verify(reminderContext.alarmManagerMock, times(1)).setExactAndAllowWhileIdle(anyInt(), anyLong(), any())
    }


}