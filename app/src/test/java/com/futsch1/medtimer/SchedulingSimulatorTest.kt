package com.futsch1.medtimer

import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.TestHelper.assertRemindedAtIndex
import com.futsch1.medtimer.TestHelper.on
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId

class SchedulingSimulatorTest {
    @Test
    fun testStandard() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 60, 1))
        val simulator = SchedulingSimulator(medicines, emptyList(), mockTimeAccess)

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate {
            scheduledReminders.add(it)
            scheduledReminders.size < 3
        }

        assertEquals(3, scheduledReminders.size)
        assertReminded(scheduledReminders, on(1, 60), medicines[0].medicine, medicines[0].reminders[0])
        assertRemindedAtIndex(scheduledReminders, on(2, 60), medicines[0].medicine, medicines[0].reminders[0], 1)
        assertRemindedAtIndex(scheduledReminders, on(3, 60), medicines[0].medicine, medicines[0].reminders[0], 2)
    }

    @Test
    fun testInterval() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 600, 1))
        medicines[0].reminders[0].intervalStart = on(1, 600).epochSecond
        val simulator = SchedulingSimulator(medicines, emptyList(), mockTimeAccess)

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate {
            scheduledReminders.add(it)
            scheduledReminders.size < 3
        }

        assertEquals(3, scheduledReminders.size)
        assertReminded(scheduledReminders, on(1, 600), medicines[0].medicine, medicines[0].reminders[0])
        assertRemindedAtIndex(scheduledReminders, on(1, 1200), medicines[0].medicine, medicines[0].reminders[0], 1)
        assertRemindedAtIndex(scheduledReminders, on(2, 360), medicines[0].medicine, medicines[0].reminders[0], 2)
    }
}