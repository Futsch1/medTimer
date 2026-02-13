package com.futsch1.medtimer.processorTests

import com.futsch1.medtimer.reminders.RepeatProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class RepeatProcessorTest {
    @Test
    fun repeat() {
        val reminderContext = TestReminderContext()
        val reminderNotificationData = fillWithTwoReminders(reminderContext)

        RepeatProcessor(reminderContext.mock).processRepeat(reminderNotificationData, 10)

        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[0].remainingRepeats, -1)
        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[1].remainingRepeats, -1)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
    }
}