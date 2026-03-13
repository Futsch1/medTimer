package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.preferences.MedTimerSettings
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

internal class ReminderSchedulerWeekendModeTest {
    @Test
    fun weekendDaysEmpty() {
        val scheduler = scheduler

        val stateFlow = MutableStateFlow(MedTimerSettings(10 * 60, true, emptySet()))

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.data
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders1)
        val scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)
    }

    @Test
    fun weekendMode() {
        // 1.1.1970 is a Thursday
        val scheduler = scheduler

        val stateFlow = MutableStateFlow(MedTimerSettings(10 * 60, true, setOf(DayOfWeek.SATURDAY.value.toString(), DayOfWeek.SUNDAY.value.toString())))

        Mockito.`when`(
            ReminderSchedulerUnitTest.preferencesDataSource.data
        ).thenReturn(stateFlow)

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders1)

        var scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList, emptyList())
        assertReminded(
            scheduledReminders, on(3, 10 * 60), medicineWithReminders1.medicine, reminder1
        )
    }
}
