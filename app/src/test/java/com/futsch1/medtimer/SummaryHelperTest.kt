package com.futsch1.medtimer

import android.content.Context
import android.text.format.DateFormat
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.reminderSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
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
}