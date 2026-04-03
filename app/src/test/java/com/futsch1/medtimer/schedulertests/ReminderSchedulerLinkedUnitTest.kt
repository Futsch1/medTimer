package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.model.reminderevent.TimeBasedReminderEvent
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.assertRemindedAtIndex
import org.junit.Test
import kotlin.test.assertEquals

class ReminderSchedulerLinkedUnitTest {
    @Test
    fun sourceReminderNotRemindedYet() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicineWithReminders)

        val reminderEventList: List<ReminderEvent> = mutableListOf()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(1, scheduledReminders.size)
        assertEquals(1, scheduledReminders[0].reminder.reminderId)
    }

    @Test
    fun scheduleLinkedReminder() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        // Reminder 1 only raised, but not processed
        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource
        )

        // Now it was also processed
        reminderEventList[0] = (reminderEventList[0] as TimeBasedReminderEvent).copy(processedTimestamp = TestHelper.on(1, 481))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(1, 541),
            medicineWithReminders.medicine,
            reminderLinked,
            0
        )
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource,
            1
        )

        val reminderEvent = TestHelper.buildReminderEvent(2, TestHelper.on(1, 541)).copy(processedTimestamp = TestHelper.on(1, 542))
        reminderEventList.add(reminderEvent)
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 480)).copy(processedTimestamp = TestHelper.on(2, 484)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 544),
            medicineWithReminders.medicine,
            reminderLinked,
            0
        )
    }

    @Test
    fun scheduleTwoLinkedReminders() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(1)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked1 = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked1.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked1)
        val reminderLinked2 = TestHelper.buildReminder(1, 3, "2", 60, 1)
        reminderLinked2.linkedReminderId = 2
        medicineWithReminders.reminders.add(reminderLinked2)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource
        )
        assertEquals(1, scheduledReminders.size)

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)).copy(processedTimestamp = TestHelper.on(1, 481)))

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(1, 541),
            medicineWithReminders.medicine,
            reminderLinked1,
            0
        )
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource,
            1
        )
        assertEquals(2, scheduledReminders.size)

        var reminderEvent = TestHelper.buildReminderEvent(2, TestHelper.on(1, 541))
        reminderEvent = reminderEvent.copy(processedTimestamp = TestHelper.on(1, 542))
        reminderEventList.add(reminderEvent)
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(1, 602),
            medicineWithReminders.medicine,
            reminderLinked2,
            0
        )
        assertRemindedAtIndex(
            scheduledReminders,
            TestHelper.on(2, 480),
            medicineWithReminders.medicine,
            reminderSource,
            1
        )
    }
}