package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDate.ofEpochDay
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ReminderSchedulerUnitTest {
    @Test
    fun scheduleEmptyLists() {
        val scheduler: ReminderScheduler = scheduler

        // Two empty lists
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(emptyList(), emptyList())
        assertTrue(scheduledReminders.isEmpty())

        // One medicine without reminders, no reminder events
        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders.toMedicine()), emptyList())
        assertTrue(scheduledReminders.isEmpty())

        // No reminder events
        val reminder = TestHelper.buildReminder(1, 1, "1", 12, 1).copy(
            createdTime = TestHelper.on(1, 13)
        )
        medicineWithReminders.reminders.add(reminder)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders.toMedicine()), emptyList())
        assertEquals(1, scheduledReminders.size)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders.toMedicine(), reminder)
    }

    @Test
    fun scheduleReminders() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders1 = TestHelper.buildTestMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        val reminder2 = TestHelper.buildReminder(1, 1, "2", 12, 1)
        medicineWithReminders1.reminders.add(reminder1)
        medicineWithReminders1.reminders.add(reminder2)
        var scheduledReminders: List<ScheduledReminder> = scheduler.schedule(listOf(medicineWithReminders1.toMedicine()), emptyList())
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 12), medicineWithReminders1.toMedicine(), reminder2)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(1, 16), medicineWithReminders1.toMedicine(), reminder1, 1)

        // Now add a second medicine with an earlier reminder
        val medicineWithReminders2 = TestHelper.buildTestMedicine(2, TEST_2)
        val reminder3 = TestHelper.buildReminder(2, 1, "1", 3, 1)
        medicineWithReminders2.reminders.add(reminder3)
        scheduledReminders = scheduler.schedule(listOf(medicineWithReminders1.toMedicine(), medicineWithReminders2.toMedicine()), emptyList())
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 3), medicineWithReminders2.toMedicine(), reminder3)
    }

    @Test
    fun scheduleWithEvents() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders1 = TestHelper.buildTestMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 12, 1)
        medicineWithReminders1.reminders.add(reminder1)
        medicineWithReminders1.reminders.add(reminder2)
        val medicineWithReminders2 = TestHelper.buildTestMedicine(2, TEST_2)
        val reminder3 = TestHelper.buildReminder(2, 3, "1", 3, 1)
        medicineWithReminders2.reminders.add(reminder3)
        val medicineWithReminders = listOf(medicineWithReminders1, medicineWithReminders2)
        // Reminder 3 already invoked
        var scheduledReminders: List<ScheduledReminder> =
            scheduler.schedule(medicineWithReminders.map { it.toMedicine() }, listOf(TestHelper.buildReminderEvent(3, TestHelper.on(2, 3))))
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders1.toMedicine(), reminder2)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(2, 16), medicineWithReminders1.toMedicine(), reminder1, 1)
        TestHelper.assertRemindedAtIndex(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.toMedicine(), reminder3, 2)

        // Check two reminders at the same time
        val reminder4 = TestHelper.buildReminder(2, 4, "1", 12, 1)
        medicineWithReminders2.reminders.add(reminder4)
        scheduledReminders = scheduler.schedule(
            medicineWithReminders.map { it.toMedicine() },
            listOf(TestHelper.buildReminderEvent(3, TestHelper.on(2, 3)), TestHelper.buildReminderEvent(2, TestHelper.on(2, 12)))
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 12), medicineWithReminders2.toMedicine(), reminder4)

        scheduledReminders = scheduler.schedule(
            medicineWithReminders.map { it.toMedicine() },
            listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4)),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12)),
                TestHelper.buildReminderEvent(4, TestHelper.on(2, 12))
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 16), medicineWithReminders1.toMedicine(), reminder1)

        // All reminders already invoked, switch to next day
        scheduledReminders = scheduler.schedule(
            medicineWithReminders.map { it.toMedicine() }, listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4).plusSeconds(4 * 60L)),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12)),
                TestHelper.buildReminderEvent(1, TestHelper.on(2, 16)),
                TestHelper.buildReminderEvent(4, TestHelper.on(2, 16))
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.toMedicine(), reminder3)

        // All reminders already invoked, we are on the next day
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))
        scheduledReminders = scheduler.schedule(
            medicineWithReminders.map { it.toMedicine() },
            listOf(
                TestHelper.buildReminderEvent(3, TestHelper.on(2, 4)),
                TestHelper.buildReminderEvent(2, TestHelper.on(2, 12)),
                TestHelper.buildReminderEvent(1, TestHelper.on(2, 16))
            )
        )
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 3), medicineWithReminders2.toMedicine(), reminder3)
    }

    // schedules a reminder for the same day
    @Test
    fun scheduleSameDayReminder() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 1).copy(
            createdTime = TestHelper.on(1, 500)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)

        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 480), medicineWithReminders.toMedicine(), reminder)
    }

    // schedules a reminder for a different medicine
    @Test
    fun scheduleDifferentMedicineReminder() {
        val scheduler: ReminderScheduler = getScheduler(1)

        val medicineWithReminders1 = TestHelper.buildTestMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 480, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineWithReminders2 = TestHelper.buildTestMedicine(2, TEST_2)
        val reminder2 = TestHelper.buildReminder(2, 2, "2", 480, 1)
        medicineWithReminders2.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders1, medicineWithReminders2)

        val reminderEventList = emptyList<ReminderEvent>()

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)

        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 480), medicineWithReminders1.toMedicine(), reminder1)
    }

    // schedules a reminder for every two days
    @Test
    fun scheduleReminderWithOneDayPause() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = mutableListOf<ReminderEvent>()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 480), medicineWithReminders.toMedicine(), reminder)

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.toMedicine(), reminder)
    }

    // schedules a reminder for every two days
    @Test
    fun scheduleTwoDayReminderVsOneDay() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1))
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 1)
        medicineWithReminders.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, 481), medicineWithReminders.toMedicine(), reminder2)
    }

    @Test
    fun scheduleReminderWithOneDayPauseVsThreeDaysPause() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 2)
        medicineWithReminders.reminders.add(reminder)
        val reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 4)
        medicineWithReminders.reminders.add(reminder2)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = mutableListOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 480)))

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(1, 481), medicineWithReminders.toMedicine(), reminder2)

        reminderEventList.add(TestHelper.buildReminderEvent(2, TestHelper.on(1, 481)))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.toMedicine(), reminder)

        // On day 3
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 480), medicineWithReminders.toMedicine(), reminder)

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(3, 480)))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.toMedicine(), reminder)

        // On day 5
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(5, 480)))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 481), medicineWithReminders.toMedicine(), reminder2)
    }

    @Test
    fun scheduleCycleInFuture() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        // Day 4 cycle start day means 5.1.
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 3).copy(
            cycleStartDay = ofEpochDay(4)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.toMedicine(), reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.toMedicine(), reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(8, 480), medicineWithReminders.toMedicine(), reminder)

        // Reminder already scheduled for tomorrow
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(6))
        reminderEventList.add(TestHelper.buildReminderEvent(1, TestHelper.on(8, 480)))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(11, 480), medicineWithReminders.toMedicine(), reminder)
    }

    @Test
    fun scheduleLongCycleInFuture() {
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler: ReminderScheduler = getScheduler(mockTimeAccess)

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 480, 0).copy(
            consecutiveDays = 90,
            pauseDays = 20,
            cycleStartDay = ofEpochDay(4)
        )
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList = emptyList<ReminderEvent>()

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.toMedicine(), reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(5, 480), medicineWithReminders.toMedicine(), reminder)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(6, 480), medicineWithReminders.toMedicine(), reminder)
    }


    @Test
    fun reminderOverMidnight() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders = TestHelper.buildTestMedicine(1, TEST)
        val reminder = TestHelper.buildReminder(1, 1, "1", 23 * 60 + 45, 1)
        medicineWithReminders.reminders.add(reminder)

        val medicineList = listOf(medicineWithReminders)

        val reminderEventList: MutableList<ReminderEvent> = mutableListOf()
        val reminderEvent = TestHelper.buildReminderEvent(1, TestHelper.on(1, (23 * 60 + 46))).copy(processedTimestamp = TestHelper.on(2, 1))
        reminderEventList.add(reminderEvent)

        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(2, (23 * 60 + 45).toLong()), medicineWithReminders.toMedicine(), reminder)
    }

    @Test
    fun reminderTomorrow() {
        val scheduler: ReminderScheduler = scheduler

        val medicineWithReminders1 = TestHelper.buildTestMedicine(1, TEST_1)
        val reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicines = listOf(medicineWithReminders1)

        val reminderEventList =
            listOf(TestHelper.buildReminderEvent(1, TestHelper.on(1, 16)), TestHelper.buildReminderEvent(1, TestHelper.on(2, 16)))

        val scheduledReminders: List<ScheduledReminder> = scheduler.schedule(medicines.map { it.toMedicine() }, reminderEventList)
        TestHelper.assertReminded(scheduledReminders, TestHelper.on(3, 16), medicineWithReminders1.toMedicine(), reminder1)
    }

    companion object {
        const val TEST_1: String = "Test1"
        const val TEST: String = "Test"
        const val TEST_2: String = "Test2"

        val scheduler: ReminderScheduler
            get() = getScheduler(0)

        var preferencesDataSource: PreferencesDataSource = Mockito.mock()

        fun getScheduler(plusDays: Int): ReminderScheduler {
            val mockTimeAccess: TimeAccess = Mockito.mock()
            Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
            Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(plusDays.toLong()))
            return getScheduler(mockTimeAccess)
        }

        fun getScheduler(timeAccess: TimeAccess): ReminderScheduler {
            val stateFlow = MutableStateFlow(UserPreferences.default())
            Mockito.`when`(preferencesDataSource.preferences).thenReturn(stateFlow)

            return ReminderScheduler(timeAccess, preferencesDataSource)
        }
    }
}
