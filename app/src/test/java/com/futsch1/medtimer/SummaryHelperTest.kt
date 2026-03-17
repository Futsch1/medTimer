package com.futsch1.medtimer

import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.futsch1.medtimer.helpers.remindersSummary
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
@HiltAndroidTest
class SummaryHelperTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    @Before
    fun setUp() {
        hiltRule.inject()
        TimeHelper.onChangedUseSystemLocale()
    }

    @Test
    fun testReminderSummaryInactive() = runBlocking {
        val reminder = Reminder(1)
        reminder.active = false

        assertEquals("Inactive, Every day", reminderSummary(reminder, RuntimeEnvironment.getApplication()))

        reminder.active = true
        reminder.periodStart = LocalDate.of(2023, 1, 2).toEpochDay()
        reminder.periodEnd = LocalDate.of(2023, 1, 4).toEpochDay()
        val dateBefore = LocalDate.of(2023, 1, 1)
        val dateIn = LocalDate.of(2023, 1, 2)
        val mockedLocalDate: MockedStatic<LocalDate> = mockStatic(LocalDate::class.java)

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateBefore)
        assertEquals("Inactive, Every day", reminderSummary(reminder, RuntimeEnvironment.getApplication()))

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateIn)
        assertEquals("Every day", reminderSummary(reminder, RuntimeEnvironment.getApplication()))

        mockedLocalDate.close()
    }

    @Test
    fun testReminderSummaryLimited() = runBlocking {
        val reminder = Reminder(1)

        reminder.days[0] = false
        assertEquals("Limited to some weekdays", reminderSummary(reminder, RuntimeEnvironment.getApplication()))

        reminder.activeDaysOfMonth = 42
        assertEquals("Limited to some weekdays, Limited to some days of the month", reminderSummary(reminder, RuntimeEnvironment.getApplication()))

        reminder.days[0] = true
        assertEquals("Limited to some days of the month, Every day", reminderSummary(reminder, RuntimeEnvironment.getApplication()))
    }

    @Test
    fun testReminderSummaryCyclic() = runBlocking {
        val preferences = MutableStateFlow(UserPreferences.default())
        Mockito.`when`(mockPreferenceDataSource.preferences).thenReturn(preferences)

        val reminder = Reminder(1)
        reminder.consecutiveDays = 4
        reminder.pauseDays = 5
        reminder.cycleStartDay = 19823
        assertEquals("Cyclic reminder 4/5, first cycle start 4/10/24", reminderSummary(reminder, RuntimeEnvironment.getApplication()))
    }

    @Test
    fun testReminderSummaryInstructions() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val reminder = Reminder(1)
        reminder.active = false
        reminder.instructions = "3"
        assertEquals("Inactive, Every day, 3", reminderSummary(reminder, context))
    }

    @Test
    fun testReminderSummaryLinked() = runBlocking {
        val context = mock<Context>()
        Mockito.`when`(context.getString(eq(R.string.linked_reminder_summary), anyString()))
            .thenReturn("1")
        val application = mock<Application>()
        Mockito.`when`(context.applicationContext).thenReturn(application)
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock<java.text.DateFormat>()
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(any(Date::class.java))).thenReturn("0:02")

        val sourceReminder = Reminder(1)
        val sourceSourceReminder = Reminder(1)
        val mockedMedicineRepositoryConstruction: MockedConstruction<MedicineRepository> =
            mockConstruction(MedicineRepository::class.java) { mock, _ ->
                runBlocking {
                    Mockito.`when`(mock.getReminder(2)).thenReturn(sourceReminder)
                    Mockito.`when`(mock.getReminder(3)).thenReturn(sourceSourceReminder)
                }
            }

        val reminder = Reminder(1)
        reminder.linkedReminderId = 2

        assertEquals("1", reminderSummary(reminder, context))

        sourceReminder.linkedReminderId = 3
        sourceReminder.timeInMinutes = 3
        assertEquals("1 + 0:03", reminderSummary(reminder, context))

        mockedMedicineRepositoryConstruction.close()
        mockedDateFormat.close()
    }

    @Test
    fun testRemindersSummarySimple() = runBlocking {
        val context = mock<Context>()
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.sum_reminders, 2, 2, "0:02; 1:03"))
            .thenReturn("ok")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock<java.text.DateFormat>()
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(TimeHelper.minutesToDate(2)))
            .thenReturn("0:02")
        Mockito.`when`(dateFormat.format(TimeHelper.minutesToDate(63)))
            .thenReturn("1:03")

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 63
        assertEquals("ok", remindersSummary(listOf(reminder2, reminder), context))

        mockedDateFormat.close()
    }

    @Test
    fun testRemindersSummaryLinked() = runBlocking {
        val context = mock<Context>()
        val application = mock<Application>()
        Mockito.`when`(context.applicationContext).thenReturn(application)
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(
            resources.getQuantityString(
                R.plurals.sum_reminders,
                3,
                3,
                "0:02; 0:02 + 1:03; 0:02 + 1:03 + 2:24"
            )
        )
            .thenReturn("ok")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock<java.text.DateFormat>()
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(TimeHelper.minutesToDate(2)))
            .thenReturn("0:02")
        Mockito.`when`(dateFormat.format(TimeHelper.minutesToDate(63)))
            .thenReturn("1:03")
        Mockito.`when`(dateFormat.format(TimeHelper.minutesToDate(144)))
            .thenReturn("2:24")

        val reminder = Reminder(1)
        reminder.reminderId = 1
        reminder.timeInMinutes = 2
        val reminder2 = Reminder(1)
        reminder2.linkedReminderId = 1
        reminder2.timeInMinutes = 63
        reminder2.reminderId = 2
        val reminder3 = Reminder(1)
        reminder3.linkedReminderId = 2
        reminder3.timeInMinutes = 144

        val mockedMedicineRepositoryConstruction: MockedConstruction<MedicineRepository> =
            mockConstruction(MedicineRepository::class.java) { mock, _ ->
                runBlocking {
                    Mockito.`when`(mock.getReminder(2)).thenReturn(reminder2)
                    Mockito.`when`(mock.getReminder(1)).thenReturn(reminder)
                }
            }

        assertEquals("ok", remindersSummary(listOf(reminder2, reminder, reminder3), context))

        mockedDateFormat.close()
        mockedMedicineRepositoryConstruction.close()
    }

    @Test
    fun testRemindersSummaryInterval() = runBlocking {
        val context = mock<Context>()
        Mockito.`when`(context.getString(R.string.every_interval, "2 ok"))
            .thenReturn("ok")
        Mockito.`when`(context.getString(R.string.continuous_from, "0 1"))
            .thenReturn("start time")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock<java.text.DateFormat>()
        val timeFormat = mock<java.text.DateFormat>()
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getDateFormat(any()) }
            .thenReturn(dateFormat)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(timeFormat)
        Mockito.`when`(dateFormat.format(Date.from(Instant.ofEpochSecond(1))))
            .thenReturn("0")
        Mockito.`when`(timeFormat.format(Date.from(Instant.ofEpochSecond(1))))
            .thenReturn("1")
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.hours, 2))
            .thenReturn("ok")
        Mockito.`when`(resources.getQuantityString(R.plurals.minutes, 2))
            .thenReturn("ok")
        Mockito.`when`(resources.getQuantityString(R.plurals.sum_reminders, 2, 2, "ok, start time; ok, start time"))
            .thenReturn("ok")

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        reminder.intervalStart = 1
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 120
        reminder2.intervalStart = 1

        assertEquals("ok, start time", reminderSummary(reminder, context))
        assertEquals("ok", remindersSummary(listOf(reminder2, reminder), context))

        mockedDateFormat.close()
    }
}
