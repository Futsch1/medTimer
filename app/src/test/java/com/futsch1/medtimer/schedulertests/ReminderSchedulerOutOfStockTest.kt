package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import kotlin.test.assertTrue

class ReminderSchedulerOutOfStockTest {
    @Test
    fun scheduleOutOfStockReminderNotDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        medicine.amount = 12.0
        var reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            outOfStockThreshold = 10.0,
            outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        medicine.amount = 5.0
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        reminder = reminder.copy(outOfStockReminderType = Reminder.OutOfStockReminderType.ALWAYS)
        medicine.reminders[0] = reminder
        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun scheduleOutOfStockReminderDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildTestMedicine(1, "Test")
        medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1).copy(
            outOfStockThreshold = 10.0,
            outOfStockReminderType = Reminder.OutOfStockReminderType.DAILY
        )
        medicine.reminders.add(reminder)

        val medicineList = mutableListOf(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        medicine.amount = 5.0
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
    }
}
