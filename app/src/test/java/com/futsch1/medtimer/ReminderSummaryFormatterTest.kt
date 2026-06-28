package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.ReminderSummaryFormatter
import com.futsch1.medtimer.core.ui.TimeFormatter
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
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
@HiltAndroidTest
class ReminderSummaryFormatterTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockAlarmManager: AlarmManager = mock()

    @BindValue
    val mockNotificationManager: NotificationManager = mock()

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    private lateinit var mockReminderRepository: ReminderRepository
    private lateinit var formatter: ReminderSummaryFormatter
    private lateinit var timeFormatter: TimeFormatter

    @Before
    fun setUp() {
        hiltRule.inject()
        mockReminderRepository = mock()
        val app = RuntimeEnvironment.getApplication()
        val preferences = MutableStateFlow(UserPreferences.default())
        `when`(mockPreferenceDataSource.preferences).thenReturn(preferences)
        timeFormatter = TimeFormatter(app, mockPreferenceDataSource, com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor(app))
        formatter = ReminderSummaryFormatter(app, mockReminderRepository, timeFormatter)
    }

    @Test
    fun testFormatReminderSummaryInactive() = runBlocking {
        var reminder = Reminder.default().copy(medicineRelId = 1, active = false)

        assertEquals("Inactive, Every day", formatter.formatReminderSummary(reminder))

        reminder = reminder.copy(active = true, periodStart = LocalDate.of(2023, 1, 2), periodEnd = LocalDate.of(2023, 1, 4))
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
        var reminder = Reminder.default().copy(days = listOf(DayOfWeek.SUNDAY))

        assertEquals("Limited to some weekdays", formatter.formatReminderSummary(reminder))

        reminder = reminder.copy(activeDaysOfMonth = listOf(1))
        assertEquals(
            "Limited to some weekdays, Limited to some days of the month",
            formatter.formatReminderSummary(reminder)
        )

        reminder = reminder.copy(days = emptyList())
        assertEquals(
            "Limited to some days of the month, Every day",
            formatter.formatReminderSummary(reminder)
        )
    }

    @Test
    fun testFormatReminderSummaryCyclic() = runBlocking {
        val reminder = Reminder.default().copy(consecutiveDays = 4, pauseDays = 5, cycleStartDay = LocalDate.of(2024, 10, 4))
        assertEquals(
            "Cyclic reminder 4/5, first cycle start 10/4/24",
            formatter.formatReminderSummary(reminder)
        )
    }

    @Test
    fun testFormatReminderSummaryInstructions() = runBlocking {
        val reminder = Reminder.default().copy(active = false, instructions = "3")
        assertEquals("Inactive, Every day, 3", formatter.formatReminderSummary(reminder))
    }

    @Test
    fun testFormatReminderSummaryLinked() {
        var sourceReminder = Reminder.default()
        val sourceSourceReminder = Reminder.default()
        runBlocking {
            `when`(mockReminderRepository.fetch(2)).thenReturn(sourceReminder)
        }
        runBlocking {
            `when`(mockReminderRepository.fetch(3)).thenReturn(sourceSourceReminder)
        }

        val reminder = Reminder.default().copy(linkedReminderId = 2)

        runBlocking {
            assertEquals("After reminder at 8:00 AM", formatter.formatReminderSummary(reminder))
        }

        sourceReminder = sourceReminder.copy(linkedReminderId = 3, time = ReminderTime(3, isDuration = true))
        runBlocking {
            `when`(mockReminderRepository.fetch(2)).thenReturn(sourceReminder)
        }
        runBlocking {
            assertEquals("After reminder at 8:00 AM + 0:03", formatter.formatReminderSummary(reminder))
        }
    }

    @Test
    fun testFormatReminderSummaryInterval() = runBlocking {
        val reminder = Reminder.default().copy(time = ReminderTime(2, isDuration = true), intervalStart = Instant.ofEpochSecond(60))

        assertEquals("Every 2 minutes, Continuous interval starting from 1/1/70 1:01 AM", formatter.formatReminderSummary(reminder))
    }
}
