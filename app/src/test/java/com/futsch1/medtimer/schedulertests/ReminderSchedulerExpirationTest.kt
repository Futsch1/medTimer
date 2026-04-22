package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertTrue

class ReminderSchedulerExpirationTest {
    @Test
    fun scheduleExpirationReminderDisabled() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        medicine.expirationDate = LocalDate.EPOCH
        var reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE,
            periodStart = LocalDate.ofEpochDay(3)
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun scheduleExpirationReminderOnce() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        medicine.expirationDate = TestHelper.on(3)
        var reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            expirationReminderType = Reminder.ExpirationReminderType.ONCE,
            periodStart = LocalDate.ofEpochDay(1)
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        reminder = reminder.copy(periodStart = LocalDate.ofEpochDay(2))
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.toMedicine(),
            reminder
        )

        reminder = reminder.copy(periodStart = LocalDate.ofEpochDay(5))
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, listOf())
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.toMedicine(),
            reminder
        )
    }

    @Test
    fun scheduleExpirationReminderDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        medicine.expirationDate = TestHelper.on(3)
        var reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            expirationReminderType = Reminder.ExpirationReminderType.DAILY,
            periodStart = LocalDate.ofEpochDay(1)
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.toMedicine(),
            reminder
        )

        reminder = reminder.copy(periodStart = LocalDate.ofEpochDay(2))
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.toMedicine(),
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.toMedicine(),
            reminder
        )

        medicine.expirationDate = TestHelper.on(5)
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 480),
            medicine.toMedicine(),
            reminder
        )

        reminder = reminder.copy(periodStart = LocalDate.ofEpochDay(14))
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.toMedicine(),
            reminder
        )
    }
}
