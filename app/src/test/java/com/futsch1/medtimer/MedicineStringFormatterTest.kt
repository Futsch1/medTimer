package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.ui.MedicineStringFormatter
import com.futsch1.medtimer.core.ui.TimeFormatter
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalTime
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
@HiltAndroidTest
class MedicineStringFormatterTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockAlarmManager: AlarmManager = mock()

    @BindValue
    val mockNotificationManager: NotificationManager = mock()

    @BindValue
    val mockPreferenceDataSource: PreferencesDataSource = mock()

    private lateinit var formatter: MedicineStringFormatter

    @Before
    fun setUp() {
        hiltRule.inject()
        val app = RuntimeEnvironment.getApplication()
        val preferences = MutableStateFlow(UserPreferences.default())
        `when`(mockPreferenceDataSource.preferences).thenReturn(preferences)
        val timeFormatter =
            TimeFormatter(app, mockPreferenceDataSource, com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor(app))
        formatter = MedicineStringFormatter(app, mockPreferenceDataSource, timeFormatter)
    }

    @Test
    fun testGetReminderTimesTimeBasedSorted() {
        val reminder = Reminder.default().copy(time = ReminderTime(LocalTime.of(0, 2)))
        val reminder2 = Reminder.default().copy(time = ReminderTime(LocalTime.of(1, 3)))
        val medicine = Medicine.default().copy(reminders = listOf(reminder2, reminder))

        assertEquals(listOf("12:02 AM", "1:03 AM"), formatter.getReminderTimes(medicine))
    }

    @Test
    fun testGetReminderTimesLinkedWithDelays() {
        val reminder = Reminder.default().copy(id = 1, time = ReminderTime(LocalTime.of(0, 2)))
        val reminder2 = Reminder.default().copy(id = 2, time = ReminderTime(63, isDuration = true), linkedReminderId = 1)
        val reminder3 = Reminder.default().copy(id = 3, time = ReminderTime(144, isDuration = true), linkedReminderId = 2)
        val medicine = Medicine.default().copy(reminders = listOf(reminder2, reminder, reminder3))

        assertEquals(
            listOf("12:02 AM", "12:02 AM + 1:03", "12:02 AM + 1:03 + 2:24"),
            formatter.getReminderTimes(medicine)
        )
    }

    @Test
    fun testGetReminderTimesInterval() {
        val reminder = Reminder.default().copy(time = ReminderTime(2, isDuration = true), intervalStart = Instant.ofEpochSecond(60))
        val reminder2 = Reminder.default().copy(time = ReminderTime(LocalTime.of(2, 0)), intervalStart = Instant.ofEpochSecond(60))
        val medicine = Medicine.default().copy(reminders = listOf(reminder2, reminder))

        assertEquals(
            listOf(
                "Every 2 hours, Continuous interval starting from 1/1/70 1:01 AM",
                "Every 2 minutes, Continuous interval starting from 1/1/70 1:01 AM"
            ),
            formatter.getReminderTimes(medicine)
        )
    }
}
