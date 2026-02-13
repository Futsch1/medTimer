package com.futsch1.medtimer.processorTests

import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.RefillProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.schedulerTests.TestHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RefillProcessorTest {
    @Test
    fun directRefill() {
        val reminderContext = TestReminderContext()

        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0

        RefillProcessor(reminderContext.reminderContextMock).processRefill(1)

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals("100 ➡ 110", reminderContext.medicineRepositoryFake.reminderEvents[0].amount)
        assertEquals(Reminder.ReminderType.REFILL, reminderContext.medicineRepositoryFake.reminderEvents[0].reminderType)
    }

    @Test
    fun refillViaEvent() {
        val reminderContext = TestReminderContext()

        reminderContext.medicineRepositoryFake.medicines.add(TestHelper.buildFullMedicine(1, "Test").medicine)
        reminderContext.medicineRepositoryFake.medicines[0].refillSizes.add(10.0)
        reminderContext.medicineRepositoryFake.medicines[0].amount = 100.0
        reminderContext.medicineRepositoryFake.reminders.add(TestHelper.buildReminder(1, 1, "1", 0, 1))
        reminderContext.medicineRepositoryFake.reminderEvents.add(TestHelper.buildReminderEvent(1, 0, 1))

        RefillProcessor(reminderContext.reminderContextMock).processRefill(ProcessedNotificationData(listOf(1)))

        assertEquals(110.0, reminderContext.medicineRepositoryFake.medicines[0].amount)
        assertEquals(ReminderEvent.ReminderStatus.ACKNOWLEDGED, reminderContext.medicineRepositoryFake.reminderEvents[0].status)
    }
}