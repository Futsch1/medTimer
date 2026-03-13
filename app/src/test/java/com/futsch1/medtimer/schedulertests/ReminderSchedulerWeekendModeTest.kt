package com.futsch1.medtimer.schedulertests

import android.content.SharedPreferences
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.di.DatastoreModule
import com.futsch1.medtimer.di.DefaultPrefs
import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.Companion.scheduler
import com.futsch1.medtimer.schedulertests.TestHelper.assertReminded
import com.futsch1.medtimer.schedulertests.TestHelper.buildFullMedicine
import com.futsch1.medtimer.schedulertests.TestHelper.buildReminder
import com.futsch1.medtimer.schedulertests.TestHelper.on
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@UninstallModules(DatastoreModule::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = dagger.hilt.android.testing.HiltTestApplication::class)
internal class ReminderSchedulerWeekendModeTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @BindValue
    @DefaultPrefs
    @Mock
    val sharedPreferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Inject
    lateinit var preferencesDataSource: MedTimerPreferencesDataSource

    @Test
    fun weekendDaysEmpty() {
        ReminderSchedulerUnitTest.preferencesDataSource = preferencesDataSource
        val scheduler = scheduler

        Mockito.`when`(
            sharedPreferences.getBoolean(
                PreferencesNames.WEEKEND_MODE, false
            )
        ).thenReturn(true)
        Mockito.`when`(
            sharedPreferences.getStringSet(
                ArgumentMatchers.eq(PreferencesNames.WEEKEND_DAYS),
                ArgumentMatchers.any<Set<String>>()
            )
        ).thenReturn(setOf<String>())

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

        Mockito.`when`(
            sharedPreferences.getBoolean(
                ArgumentMatchers.eq(
                    PreferencesNames.WEEKEND_MODE
                ), ArgumentMatchers.anyBoolean()
            )
        ).thenReturn(true)
        Mockito.`when`(
            sharedPreferences.getInt(
                ArgumentMatchers.eq(
                    PreferencesNames.WEEKEND_TIME
                ), ArgumentMatchers.anyInt()
            )
        ).thenReturn(10 * 60)
        val weekendDays = mutableSetOf<String>()
        weekendDays.add(DayOfWeek.SATURDAY.value.toString())
        weekendDays.add(DayOfWeek.SUNDAY.value.toString())
        Mockito.`when`(
            sharedPreferences.getStringSet(
                ArgumentMatchers.eq(PreferencesNames.WEEKEND_DAYS),
                ArgumentMatchers.any<Set<String>>()
            )
        ).thenReturn(weekendDays)

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
