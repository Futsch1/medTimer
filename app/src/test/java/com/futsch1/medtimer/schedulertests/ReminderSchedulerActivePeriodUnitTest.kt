package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.reminders.TimeAccess
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

internal class ReminderSchedulerActivePeriodUnitTest {
    @Test
    fun scheduleInactive() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(1)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(active = false)
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActive() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            periodStart = LocalDate.ofEpochDay(3),
            periodEnd = LocalDate.ofEpochDay(4)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(4, 480), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.toMedicine(), scheduledReminders[0].medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(5, 480), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActiveInterval() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = Instant.ofEpochSecond(60),
            periodStart = LocalDate.ofEpochDay(3),
            periodEnd = LocalDate.ofEpochDay(4)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(4, 1), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.toMedicine(), scheduledReminders[0].medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(5, 1), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActiveWindowedInterval() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = Instant.ofEpochSecond(60 * 60),
            periodStart = LocalDate.ofEpochDay(3),
            periodEnd = LocalDate.ofEpochDay(4),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(2, 0),
            intervalEndTimeOfDay = LocalTime.of(11, 40)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(4, 120), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.toMedicine(), scheduledReminders[0].medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(TestHelper.on(5, 120), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }
}
