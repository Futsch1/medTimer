package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZoneId

internal class ReminderSchedulerTimezoneUnitTest {
    @Test
    fun scheduleWithEvents() {
        val scheduler = getScheduler(1)

        val medicineWithReminders1 = TestHelper.buildFullMedicine(1, "Test1")
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 1, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineWithReminders = listOf(medicineWithReminders1)
        val reminderEvents = listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 1).epochSecond))
        Mockito.`when`(scheduler.timeAccess.systemZone()).thenReturn(ZoneId.of("CET"))
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicineWithReminders, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "CET"), medicineWithReminders1.medicine, reminder1)

        Mockito.`when`(scheduler.timeAccess.systemZone()).thenReturn(ZoneId.of("America/New_York"))
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "America/New_York"), medicineWithReminders1.medicine, reminder1)

        Mockito.`when`(scheduler.timeAccess.systemZone()).thenReturn(ZoneId.of("Asia/Kolkata"))
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "Asia/Kolkata"), medicineWithReminders1.medicine, reminder1)
    }
}