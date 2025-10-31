package com.futsch1.medtimer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.LocaleList
import android.text.format.DateFormat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.futsch1.medtimer.helpers.remindersSummary
import com.futsch1.medtimer.preferences.PreferencesNames.SYSTEM_LOCALE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

class SummaryHelperTest {

    @Test
    fun testReminderSummaryInactive() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.inactive)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.every_day)).thenReturn("2")
        val reminder = Reminder(1)
        reminder.active = false

        assertEquals("1, 2", reminderSummary(reminder, context))

        reminder.active = true
        reminder.periodStart = LocalDate.of(2023, 1, 2).toEpochDay()
        reminder.periodEnd = LocalDate.of(2023, 1, 4).toEpochDay()
        val dateBefore = LocalDate.of(2023, 1, 1)
        val dateIn = LocalDate.of(2023, 1, 2)
        val mockedLocalDate: MockedStatic<LocalDate> = mockStatic(LocalDate::class.java)

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateBefore)
        assertEquals("1, 2", reminderSummary(reminder, context))

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateIn)
        assertEquals("2", reminderSummary(reminder, context))

        mockedLocalDate.close()
    }

    @Test
    fun test_reminderSummary_limited() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.weekday_limited)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.day_of_month_limited)).thenReturn("2")
        Mockito.`when`(context.getString(R.string.every_day)).thenReturn("3")
        val reminder = Reminder(1)

        reminder.days[0] = false
        assertEquals("1", reminderSummary(reminder, context))

        reminder.activeDaysOfMonth = 42
        assertEquals("1, 2", reminderSummary(reminder, context))

        reminder.days[0] = true
        assertEquals("2, 3", reminderSummary(reminder, context))
    }

    @Test
    fun testReminderSummaryCyclic() {
        val localeList = mock(LocaleList::class.java)
        Mockito.`when`(localeList.get(0)).thenReturn(Locale.US)
        val configuration = mock(android.content.res.Configuration::class.java)
        Mockito.`when`(configuration.locales).thenReturn(localeList)
        val resources = mock(android.content.res.Resources::class.java)
        Mockito.`when`(resources.configuration).thenReturn(configuration)
        val context = mock(Context::class.java)
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(context.getString(R.string.cycle_reminder)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.cycle_start_date)).thenReturn("2")
        val preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(SYSTEM_LOCALE, false)).thenReturn(false)
        val preferencesManager = mockStatic(PreferenceManager::class.java)
        preferencesManager.`when`<Any> { PreferenceManager.getDefaultSharedPreferences(context) }.thenReturn(preferencesMock)

        val reminder = Reminder(1)
        reminder.consecutiveDays = 4
        reminder.pauseDays = 5
        reminder.cycleStartDay = 19823
        assertEquals("1 4/5, 2 4/10/24", reminderSummary(reminder, context))

        preferencesManager.close()
    }

    @Test
    fun testReminderSummaryInstructions() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.inactive)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.every_day)).thenReturn("2")
        val reminder = Reminder(1)
        reminder.active = false
        reminder.instructions = "3"
        assertEquals("1, 2, 3", reminderSummary(reminder, context))
    }

    @Test
    fun testReminderSummaryLinked() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(eq(R.string.linked_reminder_summary), anyString()))
            .thenReturn("1")
        val application = mock(Application::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(application)
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(any(Date::class.java))).thenReturn("0:02")

        val sourceReminder = Reminder(1)
        val sourceSourceReminder = Reminder(1)
        val mockedMedicineRepositoryConstruction: MockedConstruction<MedicineRepository> =
            mockConstruction(MedicineRepository::class.java) { mock, _ ->
                Mockito.`when`(
                    mock.getReminder(2)
                ).thenReturn(sourceReminder)
                Mockito.`when`(
                    mock.getReminder(3)
                ).thenReturn(sourceSourceReminder)

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
    fun testRemindersSummarySimple() {
        val context = mock(Context::class.java)
        val resources = mock(android.content.res.Resources::class.java)
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.sum_reminders, 2, 2, "0:02; 1:03"))
            .thenReturn("ok")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(0, 2))))
            .thenReturn("0:02")
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(1, 3))))
            .thenReturn("1:03")

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 63
        assertEquals("ok", remindersSummary(listOf(reminder2, reminder), context))

        mockedDateFormat.close()
    }

    @Test
    fun testRemindersSummaryLinked() {
        val context = mock(Context::class.java)
        val resources = mock(android.content.res.Resources::class.java)
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
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(0, 2))))
            .thenReturn("0:02")
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(1, 3))))
            .thenReturn("1:03")
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(2, 24))))
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
                Mockito.`when`(
                    mock.getReminder(2)
                ).thenReturn(reminder2)
                Mockito.`when`(
                    mock.getReminder(1)
                ).thenReturn(reminder)
            }

        assertEquals("ok", remindersSummary(listOf(reminder2, reminder, reminder3), context))

        mockedDateFormat.close()
        mockedMedicineRepositoryConstruction.close()
    }

    @Test
    fun testRemindersSummaryInterval() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.every_interval, "2 ok"))
            .thenReturn("ok")
        Mockito.`when`(context.getString(R.string.continuous_from, "0 1"))
            .thenReturn("start time")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        val timeFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getDateFormat(any()) }
            .thenReturn(dateFormat)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(any()) }
            .thenReturn(timeFormat)
        Mockito.`when`(dateFormat.format(Date.from(Instant.ofEpochSecond(1))))
            .thenReturn("0")
        Mockito.`when`(timeFormat.format(Date.from(Instant.ofEpochSecond(1))))
            .thenReturn("1")
        val resources = mock(android.content.res.Resources::class.java)
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