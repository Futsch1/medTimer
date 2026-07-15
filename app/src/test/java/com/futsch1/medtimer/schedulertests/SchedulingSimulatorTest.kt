package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.feature.reminders.scheduling.SchedulingSimulator
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.assertRemindedAtIndex
import com.futsch1.medtimer.schedulertests.TestHelper.on
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

class SchedulingSimulatorTest {
    private fun buildSchedulingSimulator(
        medicines: List<TestMedicine>,
        recentReminders: List<ReminderEvent>
    ): SchedulingSimulator {
        val mockTimeAccess = Mockito.mock<TimeAccess>()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val mockPreferencesDataSource = Mockito.mock<PreferencesDataSource>()
        val stateFlow = MutableStateFlow(UserPreferences.default())
        Mockito.`when`(mockPreferencesDataSource.preferences).thenReturn(stateFlow)

        return SchedulingSimulator(
            medicines.map { it.toMedicine() },
            recentReminders,
            mockTimeAccess,
            mockPreferencesDataSource
        )
    }

    @Test
    fun standard() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test"),
            TestHelper.buildTestMedicine(1, "Test2")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 60, 1))
        medicines[1].reminders.add(TestHelper.buildReminder(1, 2, "1", 120, 1))
        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, localDate: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            if (simulatedReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(0.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
            }
            simulatedReminders.size < 5
        }

        assertEquals(5, simulatedReminders.size)
        assertReminded(
            simulatedReminders,
            on(1, 60),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(1, 120),
            medicines[1].toMedicine(),
            medicines[1].reminders[0],
            1
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 60),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            2
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 120),
            medicines[1].toMedicine(),
            medicines[1].reminders[0],
            3
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(3, 60),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            4
        )
    }

    @Test
    fun interval() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        medicines[0].reminders.add(
            TestHelper.buildReminder(0, 1, "1", 600, 1).copy(
                intervalStart = on(1, 600)
            )
        )

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, _: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            simulatedReminders.size < 3
        }

        assertEquals(3, simulatedReminders.size)
        assertReminded(
            simulatedReminders,
            on(1, 600),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(1, 1200),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            1
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 360),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            2
        )
    }

    @Test
    fun intervalStartsFromProcessed() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        medicines[0].reminders.add(
            TestHelper.buildReminder(0, 1, "1", 600, 1).copy(
                intervalStart = on(1, 600),
                intervalStartsFromProcessed = true
            )
        )

        // Two recent reminder events. The first one was processed (100 minutes after the
        // reminder), the second one was raised but never processed (default
        // processedTimestamp = EPOCH). With intervalStartsFromProcessed = true, the
        // simulator must skip the unprocessed event and base the next reminder on the
        // processedTimestamp of the processed event.
        val recentReminders = listOf(
            TestHelper.buildReminderEvent(1, on(1, 600)).copy(processedTimestamp = on(1, 700)),
            TestHelper.buildReminderEvent(1, on(1, 800))
        )

        val simulator = buildSchedulingSimulator(medicines, recentReminders)

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, _: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            simulatedReminders.size < 3
        }

        assertEquals(3, simulatedReminders.size)
        // The next reminder should be remindedTimestamp of the unprocessed event (on(1, 700))
        // plus the interval (600 minutes), i.e. on(1, 1400).
        assertReminded(
            simulatedReminders,
            on(1, 1400),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 560),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            1
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 1160),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            2
        )
    }

    @Test
    fun windowedInterval() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        medicines[0].reminders.add(
            TestHelper.buildReminder(0, 1, "1", 480, 1).copy(
                intervalStart = on(1, 0),
                windowedInterval = true,
                intervalStartTimeOfDay = LocalTime.of(2, 0),
                intervalEndTimeOfDay = LocalTime.of(11, 40)
            )
        )

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, _: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            simulatedReminders.size < 3
        }

        assertEquals(3, simulatedReminders.size)
        assertReminded(
            simulatedReminders,
            on(1, 120),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(1, 600),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            1
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 120),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            2
        )
    }

    @Test
    fun windowedIntervalReversed() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        medicines[0].reminders.add(
            TestHelper.buildReminder(0, 1, "1", 741, 1).copy(
                intervalStart = on(1, 0),
                windowedInterval = true,
                intervalStartTimeOfDay = LocalTime.of(11, 40),
                intervalEndTimeOfDay = LocalTime.of(2, 0)
            )
        )

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, _: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            simulatedReminders.size < 5
        }

        assertEquals(5, simulatedReminders.size)
        assertReminded(
            simulatedReminders,
            on(1, 700),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 1),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            1
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(2, 700),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            2
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(3, 1),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            3
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(3, 700),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            4
        )
    }

    @Test
    fun linkedAndAmount() = runBlocking {
        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        medicineWithReminders.amount = 12.0
        val reminderSource = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders.reminders.add(reminderSource)
        val reminderLinked = TestHelper.buildReminder(1, 2, "2", 60, 1).copy(
            linkedReminderId = 1
        )
        medicineWithReminders.reminders.add(reminderLinked)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, localDate: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            if (simulatedReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(12.0, simulatedReminder.stockBefore)
                assertEquals(11.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 2) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(11.0, simulatedReminder.stockBefore)
                assertEquals(9.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(9.0, simulatedReminder.stockBefore)
                assertEquals(8.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 4) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(8.0, simulatedReminder.stockBefore)
                assertEquals(6.0, simulatedReminder.stockAfter)
            }
            simulatedReminders.size < 4
        }
    }

    @Test
    fun noReminders() = runBlocking {
        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        simulator.simulate { _: SimulatedReminder, localDate: LocalDate ->
            localDate == LocalDate.EPOCH.plusDays(30)
        }
    }

    @Test
    fun dailyReminders() = runBlocking {
        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            intervalStart = Instant.ofEpochSecond(60 * 60),
            windowedInterval = true,
            intervalStartsFromProcessed = false,
            intervalStartTimeOfDay = LocalTime.of(2, 0),
            intervalEndTimeOfDay = LocalTime.of(11, 40)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, localDate: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            if (simulatedReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(simulatedReminder.scheduledReminder.timestamp, Instant.ofEpochSecond(120 * 60))
            }
            if (simulatedReminders.size == 2) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(simulatedReminder.scheduledReminder.timestamp, Instant.ofEpochSecond(600 * 60))
            }
            if (simulatedReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(1), localDate)
                assertEquals(
                    simulatedReminder.scheduledReminder.timestamp,
                    Instant.ofEpochSecond(120 * 60 + 24 * 60 * 60)
                )
            }
            simulatedReminders.size < 3
        }
    }

    @Test
    fun outOfStock() = runBlocking {
        val medicineWithReminders = TestHelper.buildTestMedicine(1, "Test")
        medicineWithReminders.amount = 12.0
        val reminder = TestHelper.buildReminder(1, 1, "3", 480, 1)
        medicineWithReminders.reminders.add(reminder)
        val outOfStockReminder = TestHelper.buildReminder(1, 2, "", 479, 1).copy(
            outOfStockThreshold = 6.0,
            outOfStockReminderType = Reminder.OutOfStockReminderType.DAILY
        )
        medicineWithReminders.reminders.add(outOfStockReminder)

        val medicines = listOf(medicineWithReminders)

        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, localDate: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            if (simulatedReminders.size == 1) {
                assertEquals(LocalDate.EPOCH, localDate)
                assertEquals(12.0, simulatedReminder.stockBefore)
                assertEquals(9.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 3) {
                assertEquals(LocalDate.EPOCH.plusDays(2), localDate)
                assertEquals(6.0, simulatedReminder.stockBefore)
                assertEquals(6.0, simulatedReminder.stockAfter)
                assertEquals(simulatedReminder.scheduledReminder.reminder.reminderType, ReminderType.OUT_OF_STOCK)
            }
            simulatedReminders.size < 3
        }
    }

    @Test
    fun twoFutureEventsNotDuplicated() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        medicines[0].reminders.add(TestHelper.buildReminder(0, 1, "1", 480, 1))

        val recentReminders = listOf(
            TestHelper.buildReminderEvent(1, on(2, 480)),
            TestHelper.buildReminderEvent(1, on(3, 480))
        )

        val simulator = buildSchedulingSimulator(medicines, recentReminders)

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, _: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            simulatedReminders.size < 2
        }

        assertEquals(2, simulatedReminders.size)
        assertReminded(
            simulatedReminders,
            on(1, 480),
            medicines[0].toMedicine(),
            medicines[0].reminders[0]
        )
        assertRemindedAtIndex(
            simulatedReminders,
            on(4, 480),
            medicines[0].toMedicine(),
            medicines[0].reminders[0],
            1
        )
    }

    @Test
    fun singleDays() = runBlocking {
        val medicines = listOf(
            TestHelper.buildTestMedicine(0, "Test")
        )
        val reminder = TestHelper.buildReminder(0, 1, "1", 60, 1).copy(
            activeDaysOfMonth = listOf(5, 8)
        )
        medicines[0].reminders.add(reminder)
        val simulator = buildSchedulingSimulator(medicines, emptyList())

        val simulatedReminders = mutableListOf<SimulatedReminder>()

        simulator.simulate { simulatedReminder: SimulatedReminder, localDate: LocalDate ->
            simulatedReminders.add(simulatedReminder)
            if (simulatedReminders.size == 1) {
                assertEquals(LocalDate.EPOCH.plusDays(4), localDate)
                assertEquals(0.0, simulatedReminder.stockAfter)
            }
            if (simulatedReminders.size == 2) {
                assertEquals(LocalDate.EPOCH.plusDays(7), localDate)
            }
            simulatedReminders.size < 2
        }
    }
}
