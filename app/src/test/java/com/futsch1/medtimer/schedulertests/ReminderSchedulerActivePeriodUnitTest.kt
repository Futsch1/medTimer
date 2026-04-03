package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

internal class ReminderSchedulerActivePeriodUnitTest {
    @Test
    fun scheduleInactive() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(1)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.active = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicineEntity> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActive() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.periodStart = 3
        reminder.periodEnd = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicineEntity> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(4, 480), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(5, 480), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActiveInterval() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 60
        reminder.periodStart = 3
        reminder.periodEnd = 4
        medicineWithReminders.reminders.add(reminder)

        val medicineList: List<FullMedicineEntity> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(4, 1), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(5, 1), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }

    @Test
    fun scheduleActiveWindowedInterval() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

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

        val medicineList: List<FullMedicineEntity> = listOf(medicineWithReminders)
        val reminderEventList: List<ReminderEvent> = listOf()

        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(4, 120), scheduledReminders[0].timestamp)
        assertEquals(medicineWithReminders.medicine, scheduledReminders[0].medicine.medicine)
        assertEquals(reminder, scheduledReminders[0].reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(TestHelper.on(5, 120), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)
    }
}
