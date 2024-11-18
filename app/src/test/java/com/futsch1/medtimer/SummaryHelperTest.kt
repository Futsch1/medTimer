package com.futsch1.medtimer

import android.app.Application
import android.content.Context
import android.text.format.DateFormat
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.helpers.reminderSummary
import com.futsch1.medtimer.helpers.remindersSummary
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
import java.time.LocalTime
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

        mockedDateFormat.close()
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
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(context) }
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

        assertEquals("1", reminderSummary(context, reminder))

        sourceReminder.linkedReminderId = 3
        sourceReminder.timeInMinutes = 3
        assertEquals("1 + 0:03", reminderSummary(context, reminder))

        mockedMedicineRepositoryConstruction.close()
        mockedDateFormat.close()
    }

    @Test
    fun test_remindersSummary_simple() {
        val context = mock(Context::class.java)
        val resources = mock(android.content.res.Resources::class.java)
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.sum_reminders, 2, 2, "0:02, 1:03"))
            .thenReturn("ok")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(context) }
            .thenReturn(dateFormat)
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(0, 2))))
            .thenReturn("0:02")
        Mockito.`when`(dateFormat.format(TimeHelper.localTimeToDate(LocalTime.of(1, 3))))
            .thenReturn("1:03")

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 63
        assertEquals("ok", remindersSummary(context, listOf(reminder2, reminder)))

        mockedDateFormat.close()
    }


    @Test
    fun test_remindersSummary_linked() {
        val context = mock(Context::class.java)
        val resources = mock(android.content.res.Resources::class.java)
        Mockito.`when`(context.resources).thenReturn(resources)
        Mockito.`when`(
            resources.getQuantityString(
                R.plurals.sum_reminders,
                3,
                3,
                "0:02, 0:02 + 1:03, 0:02 + 1:03 + 2:24"
            )
        )
            .thenReturn("ok")
        val mockedDateFormat: MockedStatic<DateFormat> = mockStatic(DateFormat::class.java)
        val dateFormat = mock(java.text.DateFormat::class.java)
        mockedDateFormat.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(context) }
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

        assertEquals("ok", remindersSummary(context, listOf(reminder2, reminder, reminder3)))

        mockedDateFormat.close()
        mockedMedicineRepositoryConstruction.close()
    }
}