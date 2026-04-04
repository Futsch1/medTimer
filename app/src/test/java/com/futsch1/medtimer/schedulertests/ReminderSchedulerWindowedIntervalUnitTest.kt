package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class ReminderSchedulerWindowedIntervalUnitTest {
    @Test
    fun scheduleWindowedIntervalReminder() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 120),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 120)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 600),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600)))
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

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120)).copy(processedTimestamp = TestHelper.on(2, 121)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.medicine,
            reminder
        )

        reminderEventList[2] = reminderEventList[2].copy(processedTimestamp = TestHelper.on(2, 601))
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

        val medicineList: List<FullMedicineEntity> = listOf(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 130)))

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

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 120),
            medicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleWindowedIntervalReminderNextDay() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 600, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 1000
        reminder.intervalEndTimeOfDay = 200
        medicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = ArrayList()
        medicineList.add(medicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 1000),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 1000)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 160),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 160)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 1000),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 1000)))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 160),
            medicine.medicine,
            reminder
        )

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(3, 160)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)

        assertReminded(
            scheduledReminders,
            TestHelper.on(3, 1000),
            medicine.medicine,
            reminder
        )
    }
}
