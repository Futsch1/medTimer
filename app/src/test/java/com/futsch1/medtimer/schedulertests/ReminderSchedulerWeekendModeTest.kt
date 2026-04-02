package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.reminders.TimeAccess
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.getScheduler
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.buildFullMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
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

        val userPreferences = UserPreferences.default().copy(weekendTime = LocalTime.of(10, 0), weekendMode = true)
        val stateFlow = MutableStateFlow(userPreferences)

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.preferences
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineList = mutableListOf<FullMedicineEntity>()
        medicineList.add(medicineWithReminders1)
        val scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)
    }

    @Test
    fun weekendMode() {
        // 1.1.1970 is a Thursday
        val mockTimeAccess: TimeAccess = Mockito.mock()
        Mockito.`when`(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"))
        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH)
        val scheduler = getScheduler(mockTimeAccess)

        val userPreferences = UserPreferences.default().copy(
            weekendTime = LocalTime.of(10, 0),
            weekendMode = true,
            weekendDays = setOf(DayOfWeek.SATURDAY.value.toString(), DayOfWeek.SUNDAY.value.toString())
        )
        val stateFlow = MutableStateFlow(userPreferences)

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.preferences
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineList = mutableListOf<FullMedicineEntity>()
        medicineList.add(medicineWithReminders1)

        var scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)

        Mockito.`when`(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(
            scheduledReminders, on(3, 10 * 60), medicineWithReminders1.medicine, reminder1
        )
    }
}
