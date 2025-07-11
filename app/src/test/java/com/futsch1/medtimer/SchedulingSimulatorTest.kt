package com.futsch1.medtimer

import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.TestHelper.on
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SchedulingSimulatorTest {
    @Test
    fun testSchedulingSimulatorStandard() {
        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 60, 1))
        val simulator = SchedulingSimulator(medicines, emptyList(), LocalDate.ofEpochDay(0))

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate {
            scheduledReminders.add(it)
            scheduledReminders.size < 3
        }

        assertEquals(4, scheduledReminders.size)
        assertReminded(scheduledReminders, on(0, 60), medicines[0].medicine, medicines[0].reminders[0])
        assertReminded(scheduledReminders, on(1, 60), medicines[0].medicine, medicines[0].reminders[0])
        assertReminded(scheduledReminders, on(2, 60), medicines[0].medicine, medicines[0].reminders[0])
    }
}