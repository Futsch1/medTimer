package com.futsch1.medtimer

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
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
        val usDateFormat =
            java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.US)
        val usTimeFormat =
            java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US)
        usTimeFormat.timeZone = utc
        usDateFormat.timeZone = utc
        val dateFormatMock = mockStatic(DateFormat::class.java)
        dateFormatMock.`when`<Any> { DateFormat.getDateFormat(contextMock) }
            .thenReturn(usDateFormat)
        dateFormatMock.`when`<Any> { DateFormat.getTimeFormat(contextMock) }
            .thenReturn(usTimeFormat)

        // Standard case
        var medicine = FullMedicine()
        medicine.medicine = Medicine("Test")
        val reminder = Reminder(1)
        reminder.amount = "5"
        var instant = Instant.ofEpochSecond(0)
        var scheduledReminder = ScheduledReminder(medicine, reminder, instant)

        var result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        assertEquals("Test (5)\n1/1/70 1:00 AM", result.toString())
        result =
            formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        assertEquals("1/1/70 1:00 AM: Test (5)", result.toString())

        // Empty amount
        reminder.amount = ""
        result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        assertEquals("Test\n1/1/70 1:00 AM", result.toString())
        result =
            formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        assertEquals("1/1/70 1:00 AM: Test", result.toString())

        // Relative date/time
        Mockito.`when`(preferencesMock.getBoolean(USE_RELATIVE_DATE_TIME, false)).thenReturn(true)
        instant = Instant.ofEpochSecond(1).plusSeconds(3600)
        scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        result = formatScheduledReminderString(contextMock, scheduledReminder, preferencesMock)
        assertEquals("Test\nIn 1 hour, 2:00 AM", result.toString())
        result =
            formatScheduledReminderStringForWidget(contextMock, scheduledReminder, preferencesMock)
        assertEquals("In 1 hour, 2:00 AM: Test", result.toString())
    }
}