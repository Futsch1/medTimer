package com.futsch1.medtimer

import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.database.MedicineWithReminders
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
    fun test_scheduleIntervalReminder() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val scheduler = ReminderScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList: MutableList<MedicineWithReminders> = ArrayList()
        medicineList.add(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()

        var scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(1, 120),
            medicineWithReminders.medicine,
            reminder
        )

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(2, 120).epochSecond))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(2, 600),
            medicineWithReminders.medicine,
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
            medicineWithReminders.medicine,
            reminder
        )
    }

    @Test
    fun test_scheduleIntervalReminder_Pause() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(3))

        val scheduler = ReminderScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = TestHelper.on(1, 120).epochSecond
        reminder.intervalStartsFromProcessed = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList: MutableList<MedicineWithReminders> = ArrayList()
        medicineList.add(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = ArrayList()
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 600).epochSecond))

        val scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertReminded(
            scheduledReminders,
            TestHelper.on(4, 120),
            medicineWithReminders.medicine,
            reminder
        )
    }
}
