package com.futsch1.medtimer

import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId

class ReminderSchedulerIntervalUnitTest {
    @Test
    fun testScheduleIntervalReminder() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val scheduler = ReminderScheduler(mockTimeAccess)

        val medicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
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

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120).epochSecond))
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

        reminderEventList[0].processedTimestamp = TestHelper.on(2, 121).epochSecond
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 601),
            medicine.medicine,
            reminder
        )
    }

    @Test
    fun test_scheduleIntervalReminder_Pause() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(3))

        val scheduler = ReminderScheduler(mockTimeAccess)

        val fullMedicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
        fullMedicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600).epochSecond))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            fullMedicine.medicine,
            reminder
        )
    }

    @Test
    fun test_scheduleIntervalReminder_NotTaken() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(9))

        val scheduler = ReminderScheduler(mockTimeAccess)

        val fullMedicine = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 24 * 60 * 3, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = true
        fullMedicine.reminders.add(reminder)

        val medicineList: MutableList<FullMedicine> = ArrayList()
        medicineList.add(fullMedicine)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(5, 120).epochSecond))
        reminderEventList[0].processedTimestamp = TestHelper.on(5, 130).epochSecond

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(8, 130),
            fullMedicine.medicine,
            reminder
        )
    }
}
