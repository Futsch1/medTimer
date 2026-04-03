package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.ReminderSummaryFormatter
import com.futsch1.medtimer.helpers.TimeFormatter
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
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
@HiltAndroidTest
class ReminderSummaryFormatterTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    @BindValue
    val boundAlarmManager: AlarmManager = mock()

    @BindValue
    val boundNotificationManager: NotificationManager = mock()

    private lateinit var mockReminderRepository: ReminderRepository
    private lateinit var formatter: ReminderSummaryFormatter
    private lateinit var timeFormatter: TimeFormatter

    @Before
    fun setUp() {
        hiltRule.inject()
        mockReminderRepository = mock()
        val app = RuntimeEnvironment.getApplication()
        timeFormatter = TimeFormatter(app, mockPreferenceDataSource, com.futsch1.medtimer.helpers.LocaleContextAccessor(app))
        formatter = ReminderSummaryFormatter(app, mockReminderRepository, timeFormatter)
    }

    @Test
    fun testFormatReminderSummaryInactive() = runBlocking {
        val reminder = Reminder(1)
        reminder.active = false

        assertEquals("Inactive, Every day", formatter.formatReminderSummary(reminder))

        reminder.active = true
        reminder.periodStart = LocalDate.of(2023, 1, 2).toEpochDay()
        reminder.periodEnd = LocalDate.of(2023, 1, 4).toEpochDay()
        val dateBefore = LocalDate.of(2023, 1, 1)
        val dateIn = LocalDate.of(2023, 1, 2)
        val mockedLocalDate: MockedStatic<LocalDate> = mockStatic(LocalDate::class.java)

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateBefore)
        assertEquals("Inactive, Every day", formatter.formatReminderSummary(reminder))

        mockedLocalDate.`when`<LocalDate> { LocalDate.now() }.thenReturn(dateIn)
        assertEquals("Every day", formatter.formatReminderSummary(reminder))

        mockedLocalDate.close()
    }

    @Test
    fun testFormatReminderSummaryLimited() = runBlocking {
        val reminder = Reminder(1)

        reminder.days[0] = false
        assertEquals("Limited to some weekdays", formatter.formatReminderSummary(reminder))

        reminder.activeDaysOfMonth = 42
        assertEquals(
            "Limited to some weekdays, Limited to some days of the month",
            formatter.formatReminderSummary(reminder)
        )

        reminder.days[0] = true
        assertEquals(
            "Limited to some days of the month, Every day",
            formatter.formatReminderSummary(reminder)
        )
    }

    @Test
    fun testFormatReminderSummaryCyclic() = runBlocking {
        val preferences = MutableStateFlow(UserPreferences.default())
        Mockito.`when`(mockPreferenceDataSource.preferences).thenReturn(preferences)

        val reminder = Reminder(1)
        reminder.consecutiveDays = 4
        reminder.pauseDays = 5
        reminder.cycleStartDay = 19823
        assertEquals(
            "Cyclic reminder 4/5, first cycle start 4/10/24",
            formatter.formatReminderSummary(reminder)
        )
    }

    @Test
    fun testFormatReminderSummaryInstructions() = runBlocking {
        val reminder = Reminder(1)
        reminder.active = false
        reminder.instructions = "3"
        assertEquals("Inactive, Every day, 3", formatter.formatReminderSummary(reminder))
    }

    @Test
    fun testFormatReminderSummaryLinked() {
        val mockContext = mock<Context>()
        Mockito.`when`(mockContext.getString(Mockito.eq(R.string.linked_reminder_summary), Mockito.anyString()))
            .thenReturn("1")

        val mockTimeFormatter = mock<TimeFormatter>()
        Mockito.`when`(mockTimeFormatter.minutesToTimeString(Mockito.anyInt())).thenReturn("0:02")
        Mockito.`when`(mockTimeFormatter.minutesToDurationString(Mockito.anyInt()))
            .thenAnswer { invocation ->
                val minutes = invocation.getArgument<Int>(0)
                "${minutes / 60}:${String.format("%02d", minutes % 60)}"
            }

        val linkedFormatter = ReminderSummaryFormatter(mockContext, mockReminderRepository, mockTimeFormatter)

        val sourceReminder = Reminder(1)
        val sourceSourceReminder = Reminder(1)
        runBlocking {
            Mockito.`when`(mockReminderRepository.get(2)).thenReturn(sourceReminder)
        }
        runBlocking {
            Mockito.`when`(mockReminderRepository.get(3)).thenReturn(sourceSourceReminder)
        }

        val reminder = Reminder(1)
        reminder.linkedReminderId = 2

        runBlocking {
            assertEquals("1", linkedFormatter.formatReminderSummary(reminder))
        }

        sourceReminder.linkedReminderId = 3
        sourceReminder.timeInMinutes = 3
        runBlocking {
            assertEquals("1 + 0:03", linkedFormatter.formatReminderSummary(reminder))
        }
    }

    @Test
    fun testFormatRemindersSummarySimple() = runBlocking {
        val mockContext = mock<Context>()
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.sum_reminders, 2, 2, "0:02; 1:03"))
            .thenReturn("ok")

        val mockTimeFormatter = mock<TimeFormatter>()
        Mockito.`when`(mockTimeFormatter.minutesToTimeString(2)).thenReturn("0:02")
        Mockito.`when`(mockTimeFormatter.minutesToTimeString(63)).thenReturn("1:03")

        val simpleFormatter = ReminderSummaryFormatter(mockContext, mockReminderRepository, mockTimeFormatter)

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 63
        assertEquals("ok", simpleFormatter.formatRemindersSummary(listOf(reminder2, reminder)))
    }

    @Test
    fun testFormatRemindersSummaryLinked() = runBlocking {
        val mockContext = mock<Context>()
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(
            resources.getQuantityString(
                R.plurals.sum_reminders,
                3,
                3,
                "0:02; 0:02 + 1:03; 0:02 + 1:03 + 2:24"
            )
        )
            .thenReturn("ok")

        val mockTimeFormatter = mock<TimeFormatter>()
        Mockito.`when`(mockTimeFormatter.minutesToTimeString(2)).thenReturn("0:02")
        Mockito.`when`(mockTimeFormatter.minutesToDurationString(63)).thenReturn("1:03")
        Mockito.`when`(mockTimeFormatter.minutesToDurationString(144)).thenReturn("2:24")

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

        Mockito.`when`(mockReminderRepository.get(2)).thenReturn(reminder2)
        Mockito.`when`(mockReminderRepository.get(1)).thenReturn(reminder)

        val linkedFormatter = ReminderSummaryFormatter(mockContext, mockReminderRepository, mockTimeFormatter)

        assertEquals("ok", linkedFormatter.formatRemindersSummary(listOf(reminder2, reminder, reminder3)))
    }

    @Test
    fun testFormatRemindersSummaryInterval() = runBlocking {
        val mockContext = mock<Context>()
        Mockito.`when`(mockContext.getString(R.string.every_interval, "2 ok"))
            .thenReturn("ok")
        Mockito.`when`(mockContext.getString(R.string.continuous_from, "0 1"))
            .thenReturn("start time")
        val resources = mock<android.content.res.Resources>()
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(resources.getQuantityString(R.plurals.hours, 2))
            .thenReturn("ok")
        Mockito.`when`(resources.getQuantityString(R.plurals.minutes, 2))
            .thenReturn("ok")
        Mockito.`when`(
            resources.getQuantityString(
                R.plurals.sum_reminders,
                2,
                2,
                "ok, start time; ok, start time"
            )
        )
            .thenReturn("ok")

        val mockTimeFormatter = mock<TimeFormatter>()
        Mockito.`when`(mockTimeFormatter.secondsSinceEpochToDateTimeString(1L)).thenReturn("0 1")

        val intervalFormatter = ReminderSummaryFormatter(mockContext, mockReminderRepository, mockTimeFormatter)

        val reminder = Reminder(1)
        reminder.timeInMinutes = 2
        reminder.intervalStart = 1
        val reminder2 = Reminder(2)
        reminder2.timeInMinutes = 120
        reminder2.intervalStart = 1

        assertEquals("ok, start time", intervalFormatter.formatReminderSummary(reminder))
        assertEquals("ok", intervalFormatter.formatRemindersSummary(listOf(reminder2, reminder)))
    }
}
