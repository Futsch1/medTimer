package com.futsch1.medtimer

import android.content.Context
import android.text.format.DateFormat
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderHelperTest {

    @Test
    fun testFormatScheduledReminderString() {
        val contextMock = mock<Context>()
        val preferencesDataSourceMock = mock<PreferencesDataSource>()
        Mockito.`when`(preferencesDataSourceMock.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        val localeContextAccessor = mock<com.futsch1.medtimer.helpers.LocaleContextAccessor>()
        Mockito.`when`(localeContextAccessor.getLocaleAwareContext()).thenReturn(contextMock)
        val timeFormatter = TimeFormatter(contextMock, preferencesDataSourceMock, localeContextAccessor)

        val formatter = ReminderStringFormatter(contextMock, preferencesDataSourceMock, timeFormatter)

        val utc = TimeZone.getTimeZone("WET")
        val usDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.US)
        val usTimeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.US)
        usTimeFormat.timeZone = utc
        usDateFormat.timeZone = utc
        val dateFormatMock = mockStatic(DateFormat::class.java)
        dateFormatMock.`when`<Any> { DateFormat.getDateFormat(any()) }.thenReturn(usDateFormat)
        dateFormatMock.`when`<Any> { DateFormat.getTimeFormat(any()) }.thenReturn(usTimeFormat)
        val timeZoneMock = mockStatic(TimeZone::class.java)
        timeZoneMock.`when`<Any> { TimeZone.getDefault() }.thenReturn(utc)
        val instant = Instant.ofEpochSecond(0)
        val instantLater = instant.plusSeconds(3601)
        val instantOneDayLater = instant.plusSeconds(86400)
        val instantZero = Instant.ofEpochSecond(0)
        val instantMock = mockStatic(Instant::class.java, Mockito.CALLS_REAL_METHODS)
        instantMock.`when`<Any> { Instant.now() }.thenReturn(instant)

        // Standard case
        val medicine = FullMedicine()
        medicine.medicine = Medicine("Test")
        val reminder = Reminder(1)
        reminder.amount = "5"
        var scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        val reminderEvent = ReminderEvent()
        reminderEvent.remindedTimestamp = instant.toEpochMilli() / 1000
        reminderEvent.medicineName = "Test"
        reminderEvent.amount = "5"

        var result = formatter.formatScheduledReminder(scheduledReminder)
        var resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("1:00\u202FAM\nTest (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("1/1/70 1:00\u202FAM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("1:00\u202FAM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Empty amount
        reminder.amount = ""
        reminderEvent.amount = ""
        result = formatter.formatScheduledReminder(scheduledReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("1:00\u202FAM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("1/1/70 1:00\u202FAM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Relative date/time
        Mockito.`when`(preferencesDataSourceMock.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(useRelativeDateTime = true)))
        scheduledReminder = ScheduledReminder(medicine, reminder, instantLater)
        reminderEvent.remindedTimestamp = instantLater.toEpochMilli() / 1000
        result = formatter.formatScheduledReminder(scheduledReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("In 1 hour, 2:00\u202FAM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("In 1 hour, 2:00\u202FAM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("In 1 hour: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Widget status
        Mockito.`when`(contextMock.getString(R.string.taken)).thenReturn("Taken")
        Mockito.`when`(contextMock.getString(R.string.skipped)).thenReturn("Skipped")
        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("In 1 hour, 2:00\u202FAM: Test (Taken)", resultReminder.toString())
        reminderEvent.status = ReminderEvent.ReminderStatus.SKIPPED
        reminderEvent.amount = "6"
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("In 1 hour, 2:00\u202FAM: Test (6 Skipped)", resultReminder.toString())

        // Test show taken time in overview
        Mockito.`when`(preferencesDataSourceMock.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(showTakenTimeInOverview = true)))

        reminderEvent.status = ReminderEvent.ReminderStatus.TAKEN
        reminderEvent.remindedTimestamp = instantZero.toEpochMilli() / 1000
        reminderEvent.processedTimestamp = instantLater.toEpochMilli() / 1000
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("1:00\u202FAM ➡ 2:00\u202FAM\nTest (6)", resultReminder.toString())

        reminderEvent.processedTimestamp = instantOneDayLater.toEpochMilli() / 1000
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("1:00\u202FAM ➡ 1/2/70 1:00\u202FAM\nTest (6)", resultReminder.toString())

        // Cleanup
        dateFormatMock.close()
        timeZoneMock.close()
        instantMock.close()
    }
}
