package com.futsch1.medtimer.processorTests

import com.futsch1.medtimer.reminders.RepeatProcessor
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.schedulerTests.TestHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.time.Instant

class RepeatProcessorTest {
    @Test
    fun repeat() {
        val reminderContext = TestReminderContext()
        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 600, 1))
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 2, "1", 600, 1))
        reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1))
        reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(2, 0, 2))

        RepeatProcessor(reminderContext.reminderContextMock).processRepeat(
            ReminderNotificationData(
                Instant.ofEpochSecond(0),
                intArrayOf(1, 2),
                intArrayOf(1, 2)
            ), 10
        )

        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[0].remainingRepeats, -1)
        assertEquals(reminderContext.medicineRepositoryFake.reminderEvents[1].remainingRepeats, -1)
        verify(reminderContext.alarmManagerMock, times(1)).setAndAllowWhileIdle(anyInt(), eq(10_000L), any())
    }
}