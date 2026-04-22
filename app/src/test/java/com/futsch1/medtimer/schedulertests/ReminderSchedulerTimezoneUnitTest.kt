package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId

internal class ReminderSchedulerTimezoneUnitTest {
    @Test
    fun scheduleWithEvents() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        val scheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders1 = TestHelper.buildTestMedicine(1, "Test1")
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 1, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineWithReminders = listOf(medicineWithReminders1)
        val reminderEvents = listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 1)))
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("CET"))
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicineWithReminders.map { it.toMedicine() }, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "CET"), medicineWithReminders1.toMedicine(), reminder1)

        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("America/New_York"))
        scheduledReminders = scheduler.schedule(medicineWithReminders.map { it.toMedicine() }, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "America/New_York"), medicineWithReminders1.toMedicine(), reminder1)

        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Asia/Kolkata"))
        scheduledReminders = scheduler.schedule(medicineWithReminders.map { it.toMedicine() }, reminderEvents)
        TestHelper.assertReminded(scheduledReminders, TestHelper.onTZ(2, 1, "Asia/Kolkata"), medicineWithReminders1.toMedicine(), reminder1)
    }
}
