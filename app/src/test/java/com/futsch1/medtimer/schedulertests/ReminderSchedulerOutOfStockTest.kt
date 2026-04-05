package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import kotlin.test.assertTrue

class ReminderSchedulerOutOfStockTest {
    @Test
    fun scheduleOutOfStockReminderNotDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.outOfStockThreshold = 10.0
        reminder.outOfStockReminderType = ReminderEntity.OutOfStockReminderType.ONCE

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        medicine.medicine.amount = 5.0
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        reminder.outOfStockReminderType = ReminderEntity.OutOfStockReminderType.ALWAYS
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun scheduleOutOfStockReminderDaily() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.outOfStockThreshold = 10.0
        reminder.outOfStockReminderType = ReminderEntity.OutOfStockReminderType.DAILY

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertTrue(scheduledReminders.isEmpty())

        medicine.medicine.amount = 5.0
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )
    }
}