package com.futsch1.medtimer.processorTests

import android.app.PendingIntent
import com.futsch1.medtimer.reminders.ScheduleNextReminderNotificationProcessor
import com.futsch1.medtimer.schedulerTests.TestHelper
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ScheduleNextReminderNotificationProcessorTest {

    @Test
    fun noReminder() {
        val reminderContext = TestReminderContext()

        ScheduleNextReminderNotificationProcessor(reminderContext.mock).scheduleNextReminder()

        verify(reminderContext.alarmManagerMock, times(1)).cancel(any<PendingIntent>())
        verify(reminderContext.alarmManagerMock, never()).setAndAllowWhileIdle(anyInt(), anyLong(), any())
    }

    @Test
    fun scheduleReminder() {
        val reminderContext = TestReminderContext()
        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1))

        ScheduleNextReminderNotificationProcessor(reminderContext.mock).scheduleNextReminder()

        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(600 * 60 * 1000L), any())
    }
}