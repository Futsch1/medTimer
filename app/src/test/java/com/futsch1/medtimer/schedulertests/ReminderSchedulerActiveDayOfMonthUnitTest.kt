package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.schedulertests.TestHelper.buildFullMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.on
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

internal class ReminderSchedulerActiveDayOfMonthUnitTest {
    @Test
    fun scheduleDayOfMonth() {
        val mockTimeAccess: TimeAccess = Mockito.mock<TimeAccess>()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        val scheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = buildFullMedicine(1, "Test")
        val reminder = buildReminder(1, 1, "1", 480, 1)
        reminder.activeDaysOfMonth = 7
        medicineWithReminders.reminders.add(reminder)

        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        var scheduledReminders =
            scheduler.schedule(medicineList, reminderEventList)
        assertEquals(1, scheduledReminders.size)
        assertEquals(on(2, 480), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate())
            .thenReturn(LocalDate.EPOCH.plusDays(2))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(1, scheduledReminders.size)
        assertEquals(on(3, 480), scheduledReminders[0].timestamp)

        Mockito.`when`(mockTimeAccess.localDate())
            .thenReturn(LocalDate.EPOCH.plusDays(10))
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList)
        assertEquals(1, scheduledReminders.size)
        assertEquals(on(32, 480), scheduledReminders[0].timestamp)
    }
}
