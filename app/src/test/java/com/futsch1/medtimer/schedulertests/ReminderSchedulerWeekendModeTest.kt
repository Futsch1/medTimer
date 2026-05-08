package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.assertRemindedAtIndex
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.buildTestMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.on
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.Mockito
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal class ReminderSchedulerWeekendModeTest {
    @Test
    fun weekendDaysEmpty() {
        val scheduler = scheduler

        val userPreferences = UserPreferences.default().copy(weekendEndTime = LocalTime.of(10, 0), weekendMode = true)
        val stateFlow = MutableStateFlow(userPreferences)

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.preferences
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildTestMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineList = mutableListOf(medicineWithReminders1)
        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.toMedicine(), reminder1)
    }

    @Test
    fun weekendMode() {
        // 1.1.1970 is a Thursday
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = getScheduler(mockTimeAccess)

        val userPreferences = UserPreferences.default().copy(
            weekendEndTime = LocalTime.of(10, 0),
            weekendMode = true,
            weekendDays = setOf(DayOfWeek.SATURDAY.value.toString(), DayOfWeek.SUNDAY.value.toString())
        )
        val stateFlow = MutableStateFlow(userPreferences)

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.preferences
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildTestMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineList = mutableListOf(medicineWithReminders1)

        var scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.toMedicine(), reminder1)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, emptyList())
        assertReminded(
            scheduledReminders, on(3, 10 * 60), medicineWithReminders1.toMedicine(), reminder1
        )
    }

    @Test
    fun weekendModeTimeRange() {
        // 1.1.1970 is a Thursday; 3.1.1970 is a Saturday
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))
        val scheduler = getScheduler(mockTimeAccess)

        val userPreferences = UserPreferences.default().copy(
            weekendStartTime = LocalTime.of(4, 0),
            weekendEndTime = LocalTime.of(12, 0),
            weekendMode = true,
            weekendDays = setOf(DayOfWeek.SATURDAY.value.toString(), DayOfWeek.SUNDAY.value.toString())
        )
        val stateFlow = MutableStateFlow(userPreferences)
        Mockito.`when`(ReminderSchedulerUnitTest.preferencesDataSource.preferences).thenReturn(stateFlow)

        val medicineWithReminders1 = buildTestMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        // Reminder at 1 AM: before startTime (4 AM) → must NOT be delayed
        val reminderLateNight = buildReminder(1, 1, "1", 60, 1)
        // Reminder at 8 AM: inside range [4 AM, 12 PM] → must be delayed to 12 PM
        val reminderMorning = buildReminder(2, 1, "1", 8 * 60, 2)
        medicineWithReminders1.reminders.add(reminderLateNight)
        medicineWithReminders1.reminders.add(reminderMorning)

        val medicineList = mutableListOf(medicineWithReminders1)
        val scheduledReminders = scheduler.schedule(medicineList.map { it.toMedicine() }, emptyList())

        assertRemindedAtIndex(scheduledReminders, on(3, 1 * 60), medicineWithReminders1.toMedicine(), reminderLateNight, 0)
        assertRemindedAtIndex(scheduledReminders, on(3, 12 * 60), medicineWithReminders1.toMedicine(), reminderMorning, 1)
    }
}
