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
            TestHelper.buildFullMedicine(0, "Test"),
            TestHelper.buildFullMedicine(1, "Test2")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 60, 1))
        medicines[1].reminders.add(TestHelper.buildReminder(1, 2, "1", 120, 1))
        val simulator = SchedulingSimulator(medicines, emptyList(), mockTimeAccess)

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, localDate: LocalDate, amount: Double ->
            scheduledReminders.add(scheduledReminder)
            if (scheduledReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(0.0, amount)
            }
            if (scheduledReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
            }
            scheduledReminders.size < 5
        }

        assertEquals(5, scheduledReminders.size)
        assertReminded(scheduledReminders, on(1, 60), medicines[0].medicine, medicines[0].reminders[0])
        assertRemindedAtIndex(scheduledReminders, on(1, 120), medicines[1].medicine, medicines[1].reminders[0], 1)
        assertRemindedAtIndex(scheduledReminders, on(2, 60), medicines[0].medicine, medicines[0].reminders[0], 2)
        assertRemindedAtIndex(scheduledReminders, on(2, 120), medicines[1].medicine, medicines[1].reminders[0], 3)
        assertRemindedAtIndex(scheduledReminders, on(3, 60), medicines[0].medicine, medicines[0].reminders[0], 4)
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

        simulator.simulate { scheduledReminder: ScheduledReminder, _: LocalDate, _: Double ->
            scheduledReminders.add(scheduledReminder)
            scheduledReminders.size < 3
        }

        assertEquals(3, scheduledReminders.size)
        assertReminded(scheduledReminders, on(1, 600), medicines[0].medicine, medicines[0].reminders[0])
        assertRemindedAtIndex(scheduledReminders, on(1, 1200), medicines[0].medicine, medicines[0].reminders[0], 1)
        assertRemindedAtIndex(scheduledReminders, on(2, 360), medicines[0].medicine, medicines[0].reminders[0], 2)
    }

    @Test
    fun testLinkedAndAmount() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        medicineWithReminders.medicine.amount = 12.0
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicines = listOf(medicineWithReminders)

        val simulator = SchedulingSimulator(medicines, emptyList(), mockTimeAccess)

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, localDate: LocalDate, amount: Double ->
            scheduledReminders.add(scheduledReminder)
            if (scheduledReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(11.0, amount)
            }
            if (scheduledReminders.size == 2) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(9.0, amount)
            }
            if (scheduledReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(8.0, amount)
            }
            if (scheduledReminders.size == 4) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(6.0, amount)
            }
            scheduledReminders.size < 4
        }
    }

    @Test
    fun testNoReminders() {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)

        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val medicines = listOf(medicineWithReminders)

        val simulator = SchedulingSimulator(medicines, emptyList(), mockTimeAccess)

        simulator.simulate { _: ScheduledReminder, localDate: LocalDate, _: Double ->
            localDate == LocalDate.EPOCH.plusDays(30)
        }
    }
}