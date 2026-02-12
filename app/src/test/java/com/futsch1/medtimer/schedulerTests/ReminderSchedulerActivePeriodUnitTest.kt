package com.futsch1.medtimer.schedulerTests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate

internal class ReminderSchedulerActivePeriodUnitTest {
    @Test
    fun testScheduleInactive() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(1)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.active = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicine> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun testScheduleActive() {
        val scheduler = ReminderSchedulerUnitTest.scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.periodStart = 3
        reminder.periodEnd = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicine> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(4, 480), scheduledReminders[0].timestamp)
        Assertions.assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        Assertions.assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(5, 480), scheduledReminders[0].timestamp)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun testScheduleActiveInterval() {
        val scheduler = ReminderSchedulerUnitTest.scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 60
        reminder.periodStart = 3
        reminder.periodEnd = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicine> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(4, 1), scheduledReminders[0].timestamp)
        Assertions.assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        Assertions.assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(5, 1), scheduledReminders[0].timestamp)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun testScheduleActiveWindowedInterval() {
        val scheduler = ReminderSchedulerUnitTest.scheduler

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        reminder.periodStart = 3
        reminder.periodEnd = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicine> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(4, 120), scheduledReminders[0].timestamp)
        Assertions.assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        Assertions.assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(TestHelper.on(5, 120), scheduledReminders[0].timestamp)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertEquals(0, scheduledReminders.size)
    }
}
