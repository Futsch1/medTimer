package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.ReminderStringFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.Reminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@HiltAndroidTest
class ReminderHelperTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    @BindValue
    val boundAlarmManager: AlarmManager = mock()

    @BindValue
    val boundNotificationManager: NotificationManager = mock()

    private lateinit var mockReminderRepository: ReminderRepository
    private lateinit var formatter: ReminderStringFormatter
    private lateinit var timeFormatter: TimeFormatter

    @Before
    fun setUp() {
        hiltRule.inject()
        mockReminderRepository = org.mockito.kotlin.mock()
        val app = RuntimeEnvironment.getApplication()
        val preferences = MutableStateFlow(UserPreferences.default())
        `when`(mockPreferenceDataSource.preferences).thenReturn(preferences)
        timeFormatter = TimeFormatter(app, mockPreferenceDataSource, com.futsch1.medtimer.helpers.LocaleContextAccessor(app))
        formatter = ReminderStringFormatter(app, mockPreferenceDataSource, timeFormatter)
    }

    @Test
    fun testFormatScheduledReminderString() {
        val instant = Instant.ofEpochSecond(0)
        val instantLater = instant.plusSeconds(3601)
        val instantOneDayLater = instant.plusSeconds(86400)
        val instantZero = Instant.ofEpochSecond(0)
        val instantMock = mockStatic(Instant::class.java, Mockito.CALLS_REAL_METHODS)
        instantMock.`when`<Any> { Instant.now() }.thenReturn(instant)

        // Standard case
        val medicine = Medicine.default().copy(name = "Test")
        var reminder = Reminder.default().copy(medicineRelId = 1, amount = "5")
        var scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        var reminderEvent = ReminderEvent.default().copy(remindedTimestamp = instant, medicineName = "Test", amount = "5")

        var result = formatter.formatScheduledReminder(scheduledReminder)
        var resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM\nTest (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  1/1/70 1:00 AM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("  1:00 AM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Empty amount
        reminder = reminder.copy(amount = "")
        reminderEvent = reminderEvent.copy(amount = "")
        scheduledReminder = ScheduledReminder(medicine, reminder, instant)
        result = formatter.formatScheduledReminder(scheduledReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  1/1/70 1:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Relative date/time
        `when`(mockPreferenceDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(useRelativeDateTime = true)))
        scheduledReminder = ScheduledReminder(medicine, reminder, instantLater)
        reminderEvent = reminderEvent.copy(remindedTimestamp = instantLater)
        result = formatter.formatScheduledReminder(scheduledReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  In 1 hour, 2:00 AM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatScheduledReminderForWidget(scheduledReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("  In 1 hour: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Widget status
        reminderEvent = reminderEvent.copy(status = ReminderEvent.ReminderStatus.TAKEN)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test (Taken)", resultReminder.toString())
        reminderEvent = reminderEvent.copy(status = ReminderEvent.ReminderStatus.SKIPPED, amount = "6")
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test (6 Skipped)", resultReminder.toString())

        // Test show taken time in overview
        `when`(mockPreferenceDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default().copy(showTakenTimeInOverview = true)))

        reminderEvent = reminderEvent.copy(status = ReminderEvent.ReminderStatus.TAKEN, processedTimestamp = instantLater, remindedTimestamp = instantZero)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM ➡ 2:00 AM\nTest (6)", resultReminder.toString())

        reminderEvent = reminderEvent.copy(processedTimestamp = instantOneDayLater)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM ➡ 1/2/70 1:00 AM\nTest (6)", resultReminder.toString())

        // Cleanup
        instantMock.close()
    }
}
