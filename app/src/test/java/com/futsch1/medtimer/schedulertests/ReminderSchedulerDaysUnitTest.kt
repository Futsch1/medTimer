package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.buildFullMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.on
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ReminderSchedulerDaysUnitTest {
    @Test
    fun scheduleSkipWeekdays() {
        val scheduler = getScheduler(1)

        val medicineWithReminders = buildFullMedicine(1, "Test")
        val reminder = buildReminder(1, 1, "1", 480, 1)
        // 1.1.1970 was a Thursday, so skip the Friday and Saturday
        reminder.days[4] = false
        reminder.days[5] = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders =
            scheduler.schedule(medicineList, reminderEventList)
        // Expect it to be on the 4.1.1970
        assertEquals(on(4, 480), scheduledReminders[0].timestamp)
        assertEquals(
            medicineWithReminders.medicine,
            scheduledReminders[0].medicine.medicine
        )
        assertEquals(reminder, scheduledReminders[0].reminder)
    }

    @Test
    fun scheduleWeekdaysWithDaysBetweenReminders() {
        val scheduler = scheduler

        val medicineWithReminders = buildFullMedicine(1, "Test")
        val reminder = buildReminder(1, 1, "1", 480, 6)
        // Allow only on Mondays and only every 6 days. The start of the cycle will be on the 1.1.1970.
        reminder.days[1] = false
        reminder.days[2] = false
        reminder.days[3] = false
        reminder.days[4] = false
        reminder.days[5] = false
        reminder.days[6] = false
        medicineWithReminders.reminders.add(reminder)

        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders =
            scheduler.schedule(medicineList, reminderEventList)
        // Expect it to be on the 26.1.1970
        assertEquals(on(19, 480), scheduledReminders[0].timestamp)
        assertEquals(
            medicineWithReminders.medicine,
            scheduledReminders[0].medicine.medicine
        )
        assertEquals(reminder, scheduledReminders[0].reminder)
    }
}
