package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate

class ReminderSchedulerWindowedIntervalUnitTest {
    @Test
    fun scheduleWindowedIntervalReminder() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 120),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 120).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 600),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 120),
            medicine.medicine,
            reminder
        )

        reminder.intervalStartsFromProcessed = true
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120).epochSecond))
        reminderEventList[2].processedTimestamp = TestHelper.on(2, 121).epochSecond
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.medicine,
            reminder
        )

        reminderEventList[2].processedTimestamp = TestHelper.on(2, 601).epochSecond
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 120),
            medicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderPause() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(3)

        val fullMedicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        fullMedicine.reminders.add(reminder)

        val medicineList: List<FullMedicine> = listOf(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 130).epochSecond))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            fullMedicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderCreated() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.createdTimestamp = TestHelper.on(1, 130).epochSecond
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 120),
            medicine.medicine,
            reminder
        )
    }

}
