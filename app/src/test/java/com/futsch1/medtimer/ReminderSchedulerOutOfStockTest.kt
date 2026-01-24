package com.futsch1.medtimer

import com.futsch1.medtimer.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ReminderSchedulerOutOfStockTest {
    @Test
    fun testScheduleOutOfStockReminderNotDaily() {
        val scheduler = getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.stockThreshold = 10.0
        reminder.stockReminderType = Reminder.StockReminderType.ONCE

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())

        medicine.medicine.amount = 5.0
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())

        reminder.stockReminderType = Reminder.StockReminderType.ALWAYS
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun testScheduleOutOfStockReminderDaily() {
        val scheduler = getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.stockThreshold = 10.0
        reminder.stockReminderType = Reminder.StockReminderType.DAILY

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())

        medicine.medicine.amount = 5.0
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )
    }
}