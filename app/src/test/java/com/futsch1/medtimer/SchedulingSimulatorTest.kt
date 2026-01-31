package com.futsch1.medtimer

import android.content.SharedPreferences
import com.futsch1.medtimer.TestHelper.assertReminded
import com.futsch1.medtimer.TestHelper.assertRemindedAtIndex
import com.futsch1.medtimer.TestHelper.on
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import com.futsch1.medtimer.reminders.scheduling.SchedulingSimulator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SchedulingSimulatorTest {
    private fun buildSchedulingSimulator(medicines: List<FullMedicine>, recentReminders: List<ReminderEvent>): SchedulingSimulator {
        val mockTimeAccess = Mockito.mock(TimeAccess::class.java)
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val sharedPreferencesMock = Mockito.mock(SharedPreferences::class.java)
        Mockito.`when`(sharedPreferencesMock.getBoolean(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).thenReturn(false)

        return SchedulingSimulator(medicines, recentReminders, mockTimeAccess, sharedPreferencesMock)
    }

    @Test
    fun testStandard() {
        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test"),
            TestHelper.buildFullMedicine(1, "Test2")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 60, 1))
        medicines[1].reminders.add(TestHelper.buildReminder(1, 2, "1", 120, 1))
        val simulator = buildSchedulingSimulator(medicines, emptyList())

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
        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 600, 1))
        medicines[0].reminders[0].intervalStart = on(1, 600).epochSecond

        val simulator = buildSchedulingSimulator(medicines, emptyList())

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
    fun testWindowedInterval() {
        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 480, 1))
        medicines[0].reminders[0].intervalStart = on(1, 0).epochSecond
        medicines[0].reminders[0].windowedInterval = true
        medicines[0].reminders[0].intervalStartTimeOfDay = 120
        medicines[0].reminders[0].intervalEndTimeOfDay = 700

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, _: LocalDate, _: Double ->
            scheduledReminders.add(scheduledReminder)
            scheduledReminders.size < 3
        }

        assertEquals(3, scheduledReminders.size)
        assertReminded(scheduledReminders, on(1, 120), medicines[0].medicine, medicines[0].reminders[0])
        assertRemindedAtIndex(scheduledReminders, on(1, 600), medicines[0].medicine, medicines[0].reminders[0], 1)
        assertRemindedAtIndex(scheduledReminders, on(2, 120), medicines[0].medicine, medicines[0].reminders[0], 2)
    }

    @Test
    fun testLinkedAndAmount() {
        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        medicineWithReminders.medicine.amount = 12.0
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1)
        reminderLinked.linkedReminderId = 1
        medicineWithReminders.reminders.add(reminderLinked)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

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
        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        simulator.simulate { _: ScheduledReminder, localDate: LocalDate, _: Double ->
            localDate == LocalDate.EPOCH.plusDays(30)
        }
    }

    @Test
    fun testDailyReminders() {
        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1)
        reminder.intervalStart = 1
        reminder.windowedInterval = true
        reminder.intervalStartsFromProcessed = false
        reminder.intervalStartTimeOfDay = 120
        reminder.intervalEndTimeOfDay = 700
        medicineWithReminders.reminders.add(reminder)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, localDate: LocalDate, _: Double ->
            scheduledReminders.add(scheduledReminder)
            if (scheduledReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(scheduledReminder.timestamp, Instant.ofEpochSecond(120 * 60))
            }
            if (scheduledReminders.size == 2) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(scheduledReminder.timestamp, Instant.ofEpochSecond(600 * 60))
            }
            if (scheduledReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(scheduledReminder.timestamp, Instant.ofEpochSecond(120 * 60 + 24 * 60 * 60))
            }
            scheduledReminders.size < 3
        }
    }

    @Test
    fun testOutOfStock() {
        val medicineWithReminders = TestHelper.buildFullMedicine(1, "Test")
        medicineWithReminders.medicine.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "3", 480, 1)
        medicineWithReminders.reminders.add(reminder)
        val outOfStockReminder = TestHelper.buildReminder(1, 2, "", 481, 1)
        outOfStockReminder.outOfStockThreshold = 6.0
        outOfStockReminder.outOfStockReminderType = Reminder.OutOfStockReminderType.DAILY
        medicineWithReminders.reminders.add(outOfStockReminder)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, localDate: LocalDate, amount: Double ->
            scheduledReminders.add(scheduledReminder)
            if (scheduledReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(9.0, amount)
            }
            if (scheduledReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(6.0, amount)
                assertEquals(scheduledReminder.reminder().reminderType, Reminder.ReminderType.OUT_OF_STOCK)
            }
            scheduledReminders.size < 3
        }
    }

    @Test
    fun testSingleDays() {
        val medicines = listOf(
            TestHelper.buildFullMedicine(0, "Test")
        )
        val reminder = TestHelper.buildReminder(0, 1, "1", 60, 1)
        reminder.activeDaysOfMonth = (1 shl 5) or (1 shl 8)
        medicines[0].reminders.add(reminder)
        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val scheduledReminders = mutableListOf<ScheduledReminder>()

        simulator.simulate { scheduledReminder: ScheduledReminder, localDate: LocalDate, amount: Double ->
            scheduledReminders.add(scheduledReminder)
            if (scheduledReminders.size == 1) {
                assertEquals(LocalDate.EPOCH.plusDays(5), localDate)
                assertEquals(0.0, amount)
            }
            if (scheduledReminders.size == 2) {
                assertEquals(LocalDate.EPOCH.plusDays(8), localDate)
            }
            scheduledReminders.size < 2
        }
    }
}