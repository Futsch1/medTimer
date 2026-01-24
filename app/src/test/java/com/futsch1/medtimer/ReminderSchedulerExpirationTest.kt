package com.futsch1.medtimer

import com.futsch1.medtimer.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ReminderSchedulerExpirationTest {
    @Test
    fun testScheduleExpirationReminderDisabled() {
        val scheduler = getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = 0
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = Reminder.ExpirationReminderType.ONCE
        reminder.periodStart = 3

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun testScheduleExpirationReminderOnce() {
        val scheduler = getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = TestHelper.on(3).toEpochDay()
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = Reminder.ExpirationReminderType.ONCE
        reminder.periodStart = 1

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )

        reminder.periodStart = 2
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 480),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        Assertions.assertTrue(scheduledReminders.isEmpty())
    }

    @Test
    fun testScheduleExpirationReminderDaily() {
        val scheduler = getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        medicine.medicine.expirationDate = TestHelper.on(3).toEpochDay()
        val reminder = TestHelper.buildReminder(1, 1, "", 480, 1)
        reminder.expirationReminderType = Reminder.ExpirationReminderType.DAILY
        reminder.periodStart = 1

        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicine.medicine,
            reminder
        )

        reminder.periodStart = 2
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

        medicine.medicine.expirationDate = TestHelper.on(5).toEpochDay()
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 480),
            medicine.medicine,
            reminder
        )
    }
}