package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.buildTestMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.on
import org.junit.Test
import java.time.DayOfWeek
import kotlin.test.assertEquals

internal class ReminderSchedulerDaysUnitTest {
    @Test
    fun scheduleSkipWeekdays() {
        val scheduler = getScheduler(1)

        val medicineWithReminders = buildTestMedicine(1, "Test")
        // 1.1.1970 was a Thursday, so skip the Friday and Saturday
        val reminder = buildReminder(1, 1, "1", 480, 1).copy(
            days = DayOfWeek.entries - DayOfWeek.FRIDAY - DayOfWeek.SATURDAY
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = mutableListOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders =
            scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        // Expect it to be on the 4.1.1970
        assertEquals(on(4, 480), scheduledReminders[0].timestamp)
        assertEquals(
            medicineWithReminders.toMedicine(),
            scheduledReminders[0].medicine
        )
        assertEquals(reminder, scheduledReminders[0].reminder)
    }

    @Test
    fun scheduleWeekdaysWithDaysBetweenReminders() {
        val scheduler = scheduler

        val medicineWithReminders = buildTestMedicine(1, "Test")
        // Allow only on Mondays and only every 6 days. The start of the cycle will be on the 1.1.1970.
        val reminder = buildReminder(1, 1, "1", 480, 6).copy(days = listOf(DayOfWeek.MONDAY))
        medicineWithReminders.reminders.add(reminder)

        val medicineList = mutableListOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders =
            scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        // Expect it to be on the 26.1.1970
        assertEquals(on(19, 480), scheduledReminders[0].timestamp)
        assertEquals(
            medicineWithReminders.toMedicine(),
            scheduledReminders[0].medicine
        )
        assertEquals(reminder, scheduledReminders[0].reminder)
    }
}
