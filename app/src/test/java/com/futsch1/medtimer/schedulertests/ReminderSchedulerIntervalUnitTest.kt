package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.model.reminderevent.TimeBasedReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class ReminderSchedulerIntervalUnitTest {
    @Test
    fun scheduleIntervalReminder() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = ReminderSchedulerUnitTest.getScheduler(mockTimeAccess)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
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

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120)))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 600),
            medicine.medicine,
            reminder
        )

        reminder.intervalStartsFromProcessed = true
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(0, scheduledReminders.size)

        reminderEventList[0] = (reminderEventList[0] as TimeBasedReminderEvent).copy(processedTimestamp = TestHelper.on(2, 121))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleIntervalReminderPause() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(3)

        val fullMedicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
        fullMedicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600)))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            fullMedicine.medicine,
            reminder
        )
    }

    @Test
    fun scheduleIntervalReminderNotTaken() {
        val scheduler = ReminderSchedulerUnitTest.getScheduler(0)

        val fullMedicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 24 * 60 * 3, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = true
        fullMedicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicineEntity> = mutableListOf()
        medicineList.add(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(5, 120)).copy(processedTimestamp = TestHelper.on(5, 130)))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(8, 130),
            fullMedicine.medicine,
            reminder
        )
    }
}
