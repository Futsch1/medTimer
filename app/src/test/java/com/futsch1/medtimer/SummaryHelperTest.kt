package com.futsch1.medtimer

import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.reminderSummary
import com.google.android.material.timepicker.TimeFormat
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
import java.time.LocalDate
import java.util.Date

class SummaryHelperTest {
    @Test
    fun test_reminderSummary_inactive() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.inactive)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.every_day)).thenReturn("2")
        val reminder = Reminder(1)
        reminder.active = false

        assertEquals("1, 2", reminderSummary(context, reminder))

        reminder.active = true
        reminder.periodStart = LocalDate.of(2023, 1, 2).toEpochDay()
        reminder.periodEnd = LocalDate.of(2023, 1, 4).toEpochDay()
        val dateBefore = LocalDate.of(2023, 1, 1)
        val dateIn = LocalDate.of(2023, 1, 2)
        val mockedLocalDate: MockedStatic<LocalDate> = mockStatic(LocalDate::class.java)

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateBefore)
        assertEquals("1, 2", reminderSummary(context, reminder))

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateIn)
        assertEquals("2", reminderSummary(context, reminder))

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
        assertEquals("1", reminderSummary(context, reminder))

        reminder.activeDaysOfMonth = 42
        assertEquals("1, 2", reminderSummary(context, reminder))

        reminder.days[0] = true
        assertEquals("2, 3", reminderSummary(context, reminder))
    }

    @Test
    fun test_reminderSummary_cyclic() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.cycle_reminders)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.cycle_start_date)).thenReturn("2")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getDateFormat(context) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(any(Date::class.java))).thenReturn("19823")

        val reminder = Reminder(1)
        reminder.consecutiveDays = 4
        reminder.pauseDays = 5
        reminder.cycleStartDay = 19823
        assertEquals("1 4/5, 2 19823", reminderSummary(context, reminder))
    }

    @Test
    fun test_reminderSummary_instructions() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.inactive)).thenReturn("1")
        Mockito.`when`(context.getString(R.string.every_day)).thenReturn("2")
        val reminder = Reminder(1)
        reminder.active = false
        reminder.instructions = "3"
        assertEquals("1, 2, 3", reminderSummary(context, reminder))
    }

    @Test
    fun test_reminderSummary_linked() {
        val context = mock(Context::class.java)
        Mockito.`when`(context.getString(eq(R.string.linked_reminder_summary), anyString()))
            .thenReturn("1")
        val application = mock(Application::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(application)
        val mockedTimeFormat: MockedStatic<TimeFormat> = mockStatic(TimeFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedTimeFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(context) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(any(Date::class.java))).thenReturn("0:02")

        val sourceReminder = Reminder(1)
        val sourceSourceReminder = Reminder(1)
        val mockedMedicineRepositoryConstruction: MockedConstruction<MedicineRepository> =
            mockConstruction(MedicineRepository::class.java) { mock, _ ->
                Mockito.`when`(
                    mock.getReminder(
                        2
                    )
                ).thenReturn(sourceReminder)
                Mockito.`when`(
                    mock.getReminder(
                        3
                    )
                ).thenReturn(sourceSourceReminder)

            }

        val reminder = Reminder(1)
        reminder.linkedReminderId = 2

        assertEquals("1", reminderSummary(context, reminder))

        sourceReminder.linkedReminderId = 3
        sourceReminder.timeInMinutes = 3
        assertEquals("1 + 00:03", reminderSummary(context, reminder))

        mockedMedicineRepositoryConstruction.close()
    }
}