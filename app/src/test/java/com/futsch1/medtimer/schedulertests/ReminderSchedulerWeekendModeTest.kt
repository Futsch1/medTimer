package com.futsch1.medtimer.schedulertests

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.buildFullMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.on
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.DayOfWeek
import java.time.LocalDate

internal class ReminderSchedulerWeekendModeTest {
    @Test
    fun weekendDaysEmpty() {
        val scheduler = scheduler

        Mockito.`when`(
            scheduler.sharedPreferences.getBoolean(
                PreferencesNames.WEEKEND_MODE, false
            )
        ).thenReturn(true)
        Mockito.`when`(
            scheduler.sharedPreferences.getStringSet(
                ArgumentMatchers.eq(PreferencesNames.WEEKEND_DAYS),
                ArgumentMatchers.any<Set<String>>()
            )
        ).thenReturn(setOf<String>())

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.Companion.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)

        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders1)
        val scheduledReminders = scheduler.schedule(medicineList, ArrayList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)
    }

    @Test
    fun weekendMode() {
        // 1.1.1970 is a Thursday
        val scheduler = scheduler

        Mockito.`when`(
            scheduler.sharedPreferences.getBoolean(
                ArgumentMatchers.eq(
                    PreferencesNames.WEEKEND_MODE
                ), ArgumentMatchers.anyBoolean()
            )
        ).thenReturn(true)
        Mockito.`when`(
            scheduler.sharedPreferences.getInt(
                ArgumentMatchers.eq(
                    PreferencesNames.WEEKEND_TIME
                ), ArgumentMatchers.anyInt()
            )
        ).thenReturn(10 * 60)
        val weekendDays = mutableSetOf<String>()
        weekendDays.add(DayOfWeek.SATURDAY.value.toString())
        weekendDays.add(DayOfWeek.SUNDAY.value.toString())
        Mockito.`when`(
            scheduler.sharedPreferences.getStringSet(
                ArgumentMatchers.eq(PreferencesNames.WEEKEND_DAYS),
                ArgumentMatchers.any<Set<String>>()
            )
        ).thenReturn(weekendDays)

        val medicineWithReminders1 =
            buildFullMedicine(1, ReminderSchedulerUnitTest.Companion.TEST_1)
        val reminder1 = buildReminder(1, 1, "1", 16, 1)
        medicineWithReminders1.reminders.add(reminder1)
        val medicineList = mutableListOf<FullMedicine>()
        medicineList.add(medicineWithReminders1)

        var scheduledReminders = scheduler.schedule(medicineList, ArrayList())
        assertReminded(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1)

        Mockito.`when`(scheduler.timeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2))

        scheduledReminders = scheduler.schedule(medicineList, ArrayList())
        assertReminded(
            scheduledReminders, on(3, 10 * 60), medicineWithReminders1.medicine, reminder1
        )
    }
}

