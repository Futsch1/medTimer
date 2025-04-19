package com.futsch1.medtimer

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.formatReminderString
import com.futsch1.medtimer.helpers.formatReminderStringForWidget
import com.futsch1.medtimer.helpers.formatScheduledReminderString
import com.futsch1.medtimer.helpers.formatScheduledReminderStringForWidget
import com.futsch1.medtimer.preferences.PreferencesNames.USE_RELATIVE_DATE_TIME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34])
class ReminderHelperTest {

    @Test
    fun testFormatScheduledReminderString() {
        var contextMock = mock(Context::class.java)
        var preferencesMock = mock(SharedPreferences::class.java)
        Mockito.`when`(preferencesMock.getBoolean(USE_RELATIVE_DATE_TIME, false)).thenReturn(false)

        val utc = TimeZone.getTimeZone("WET")
        val usDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.US)
        val usTimeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US)
        usTimeFormat.timeZone = utc
        usDateFormat.timeZone = utc
        val dateFormatMock = mockStatic(DateFormat::class.java)
        dateFormatMock.`when`<Any> { DateFormat.getDateFormat(contextMock) }.thenReturn(usDateFormat)
        dateFormatMock.`when`<Any> { DateFormat.getTimeFormat(contextMock) }.thenReturn(usTimeFormat)
        val timeZoneMock = mockStatic(TimeZone::class.java)
        timeZoneMock.`when`<Any> { TimeZone.getDefault() }.thenReturn(utc)

        // Standard case
        var medicine = FullMedicine()
        medicine.medicine = Medicine("Test")
        val reminder = Reminder(1)
        reminder.amount = "5"
        var instant = Instant.ofEpochSecond(0)
        var scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        var reminderEvent = ReminderEvent()
        reminderEvent.remindedTimestamp = instant.toEpochMilli() / 1000
        reminderEvent.medicineName = "Test"
        reminderEvent.amount = "5"

        var result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        var resultReminder = formatReminderString(contextMock, reminderEvent, preferencesMock)
        assertEquals("Test (5)\n1/1/70 1:00 AM", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result =
            formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        resultReminder = formatReminderStringForWidget(contextMock, reminderEvent, preferencesMock)
        assertEquals("1/1/70 1:00 AM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Empty amount
        reminder.amount = ""
        reminderEvent.amount = ""
        result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        resultReminder = formatReminderString(contextMock, reminderEvent, preferencesMock)
        assertEquals("Test\n1/1/70 1:00 AM", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        resultReminder = formatReminderStringForWidget(contextMock, reminderEvent, preferencesMock)
        assertEquals("1/1/70 1:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Relative date/time
        Mockito.`when`(preferencesMock.getBoolean(USE_RELATIVE_DATE_TIME, false)).thenReturn(true)
        instant = Instant.ofEpochSecond(1).plusSeconds(3600)
        scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        reminderEvent.remindedTimestamp = instant.toEpochMilli() / 1000
        result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        resultReminder = formatReminderString(contextMock, reminderEvent, preferencesMock)
        assertEquals("Test\nIn 1 hour, 1:00 AM", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        resultReminder = formatReminderStringForWidget(contextMock, reminderEvent, preferencesMock)
        assertEquals("In 1 hour, 1:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Widget status
        Mockito.`when`(contextMock.getString(R.string.taken)).thenReturn("Taken")
        Mockito.`when`(contextMock.getString(R.string.skipped)).thenReturn("Skipped")
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
        resultReminder = formatReminderStringForWidget(contextMock, reminderEvent, preferencesMock)
        assertEquals("In 1 hour, 1:00 AM: Test (Taken)", resultReminder.toString())
        reminderEvent.status = ReminderEvent.ReminderStatus.SKIPPED
        reminderEvent.amount = "6"
        resultReminder = formatReminderStringForWidget(contextMock, reminderEvent, preferencesMock)
        assertEquals("In 1 hour, 1:00 AM: Test (6 Skipped)", resultReminder.toString())
    }
}