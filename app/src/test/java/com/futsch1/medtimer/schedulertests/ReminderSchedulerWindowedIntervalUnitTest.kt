package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

class ReminderSchedulerWindowedIntervalUnitTest {
    @Test
    fun scheduleWindowedIntervalReminder() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        var reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = Instant.ofEpochSecond(1),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(2, 0),
            intervalEndTimeOfDay = LocalTime.of(11, 40)
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

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 120)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 600),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 120),
            medicine.toMedicine(),
            reminder
        )

        reminder = reminder.copy(intervalStartsFromProcessed = true)
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120)).copy(processedTimestamp = TestHelper.on(2, 121)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList[2] = reminderEventList[2].copy(processedTimestamp = TestHelper.on(2, 601))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 120),
            medicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderPause() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(3)

        val fullMedicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = Instant.ofEpochSecond(1),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(2, 0),
            intervalEndTimeOfDay = LocalTime.of(11, 40)
        )
        fullMedicine.reminders.add(reminder)

        val medicineList = listOf(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 130)))

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            fullMedicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderCreated() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            createdTime = TestHelper.on(1, 130),
            intervalStart = Instant.ofEpochSecond(1),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(2, 0),
            intervalEndTimeOfDay = LocalTime.of(11, 40)
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 120),
            medicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderNextDay() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 600, 1).copy(
            intervalStart = Instant.ofEpochSecond(1),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(16, 40),
            intervalEndTimeOfDay = LocalTime.of(3, 20)
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 1000),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 1000)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 160),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 160)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 1000),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 1000)))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 160),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(3, 160)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 1000),
            medicine.toMedicine(),
            reminder
        )
    }
}
