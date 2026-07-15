package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderSchedulerIntervalUnitTest {
    @Test
    fun scheduleIntervalReminder() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        var reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = TestHelper.on(1, 120),
            intervalStartsFromProcessed = false
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 120),
            medicine.toMedicine(),
            reminder
        )

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 600),
            medicine.toMedicine(),
            reminder
        )

        reminder = reminder.copy(intervalStartsFromProcessed = true)
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)

        reminderEventList[0] = reminderEventList[0].copy(processedTimestamp = TestHelper.on(2, 121))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleIntervalReminderPause() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(3)

        val fullMedicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = TestHelper.on(1, 120),
            intervalStartsFromProcessed = false
        )
        fullMedicine.reminders.add(reminder)

        val medicineList = mutableListOf(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600)))

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            fullMedicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleIntervalReminderNotTaken() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val fullMedicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 23 * 60, 1).copy(
            intervalStart = TestHelper.on(1, 120),
            intervalStartsFromProcessed = true
        )
        fullMedicine.reminders.add(reminder)

        val medicineList = mutableListOf(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 60)).copy(processedTimestamp = TestHelper.on(2, 60)))

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 0),
            fullMedicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun intervalStartsFromProcessedDoesNotLoopOnUnprocessedEvent() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 60, 1).copy(
            intervalStart = java.time.Instant.ofEpochSecond(1),
            intervalStartsFromProcessed = true
        )
        medicine.reminders.add(reminder)

        val processedTime = TestHelper.on(1, 120)
        val processedEvent = TestHelper.buildReminderEvent(1, processedTime).copy(
            processedTimestamp = processedTime,
            status = ReminderEvent.ReminderStatus.TAKEN
        )

        var scheduledReminders = scheduler.schedule(
            listOf(medicine.toMedicine()),
            listOf(processedEvent)
        )
        assertEquals(1, scheduledReminders.size)
        assertEquals(processedTime.plusSeconds(60 * 60L), scheduledReminders[0].timestamp)

        val unprocessedEvent = TestHelper.buildReminderEvent(1, processedTime.plusSeconds(60 * 60L))
        scheduledReminders = scheduler.schedule(
            listOf(medicine.toMedicine()),
            listOf(processedEvent, unprocessedEvent)
        )

        assertTrue(
            scheduledReminders.isEmpty(),
            "An unprocessed event at the next interval must block re-scheduling, " +
                    "otherwise the notification loop would fire continuously"
        )
    }
}
