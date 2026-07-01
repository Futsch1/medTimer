package com.futsch1.medtimer

import android.app.AlarmManager
import android.app.NotificationManager
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.SimulatedReminder
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.ReminderStringFormatter
import com.futsch1.medtimer.core.ui.TimeFormatter
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
import java.time.LocalDate
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
        timeFormatter = TimeFormatter(
            app,
            mockPreferenceDataSource,
            com.futsch1.medtimer.core.common.helpers.LocaleContextAccessor(app)
        )
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
        var simulatedReminder = SimulatedReminder(ScheduledReminder(medicine, reminder, instant), 0.0, 0.0)
        var reminderEvent = ReminderEvent.default()
            .copy(remindedTimestamp = instant, medicineName = "Test", amount = "5")

        var result = formatter.formatSimulatedReminder(simulatedReminder)
        var resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM\nTest (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatSimulatedReminderForWidget(simulatedReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  1/1/70 1:00 AM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatSimulatedReminderForWidget(simulatedReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("  1:00 AM: Test (5)", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Empty amount
        reminder = reminder.copy(amount = "")
        reminderEvent = reminderEvent.copy(amount = "")
        simulatedReminder = SimulatedReminder(ScheduledReminder(medicine, reminder, instant), 0.0, 0.0)
        result = formatter.formatSimulatedReminder(simulatedReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatSimulatedReminderForWidget(simulatedReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  1/1/70 1:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Relative date/time
        `when`(mockPreferenceDataSource.preferences).thenReturn(
            MutableStateFlow(
                UserPreferences.default().copy(useRelativeDateTime = true)
            )
        )
        simulatedReminder = SimulatedReminder(ScheduledReminder(medicine, reminder, instantLater), 0.0, 0.0)
        reminderEvent = reminderEvent.copy(remindedTimestamp = instantLater)
        result = formatter.formatSimulatedReminder(simulatedReminder)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  In 1 hour, 2:00 AM\nTest", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatSimulatedReminderForWidget(simulatedReminder, false)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())
        result = formatter.formatSimulatedReminderForWidget(simulatedReminder, true)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, true)
        assertEquals("  In 1 hour: Test", result.toString())
        assertEquals(result.toString(), resultReminder.toString())

        // Widget status
        reminderEvent = reminderEvent.copy(status = ReminderEvent.ReminderStatus.TAKEN)
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test (Taken)", resultReminder.toString())
        reminderEvent =
            reminderEvent.copy(status = ReminderEvent.ReminderStatus.SKIPPED, amount = "6")
        resultReminder = formatter.formatReminderForWidget(reminderEvent, false)
        assertEquals("  In 1 hour, 2:00 AM: Test (6 Skipped)", resultReminder.toString())

        // Test show taken time in overview
        `when`(mockPreferenceDataSource.preferences).thenReturn(
            MutableStateFlow(
                UserPreferences.default().copy(showTakenTimeInOverview = true)
            )
        )

        reminderEvent = reminderEvent.copy(
            status = ReminderEvent.ReminderStatus.TAKEN,
            processedTimestamp = instantLater,
            remindedTimestamp = instantZero
        )
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM ➡ 2:00 AM\nTest (6)", resultReminder.toString())

        reminderEvent = reminderEvent.copy(processedTimestamp = instantOneDayLater)
        resultReminder = formatter.formatReminderEvent(reminderEvent)
        assertEquals("  1:00 AM ➡ 1/2/70 1:00 AM\nTest (6)", resultReminder.toString())

        // Cleanup
        instantMock.close()
    }

    @Test
    fun testFormatReminderEventStockChange() {
        val instant = Instant.ofEpochSecond(0)

        // Default: stockBefore == stockAfter == -1.0 → no stock text
        var reminderEvent = ReminderEvent.default().copy(
            remindedTimestamp = instant, medicineName = "Test", amount = "5"
        )
        assertEquals("  1:00 AM\nTest (5)", formatter.formatReminderEvent(reminderEvent).toString())

        // Stock deducted → show stockBefore ➡ stockAfter with unit
        reminderEvent =
            reminderEvent.copy(stockBefore = 10.0, stockAfter = 9.0, stockUnit = "tablets")
        assertEquals(
            "  1:00 AM,   10 tablets ➡ 9 tablets\nTest (5)",
            formatter.formatReminderEvent(reminderEvent).toString()
        )

        // stockBefore == stockAfter (e.g. skipped, stock restored) → no stock text
        reminderEvent = reminderEvent.copy(stockBefore = 9.0, stockAfter = 9.0)
        assertEquals("  1:00 AM\nTest (5)", formatter.formatReminderEvent(reminderEvent).toString())
    }

    @Test
    fun testFormatScheduledReminderStockAndExpiration() {
        val instant = Instant.ofEpochSecond(0)
        val reminder = Reminder.default().copy(amount = "5")

        // No stock management (amount == 0.0) → no expected stock text
        var medicine = Medicine.default().copy(name = "Test")
        assertEquals(
            "  1:00 AM\nTest (5)",
            formatter.formatSimulatedReminder(SimulatedReminder(ScheduledReminder(medicine, reminder, instant), 0.0, 0.0))
                .toString()
        )

        // Stock unchanged (scheduler path) → show stockBefore only
        medicine = medicine.copy(amount = 9.0, unit = "tablets")
        assertEquals(
            "  1:00 AM,   9 tablets\nTest (5)",
            formatter.formatSimulatedReminder(SimulatedReminder(ScheduledReminder(medicine, reminder, instant), 9.0, 9.0))
                .toString()
        )

        // Stock depleted (simulator path) → show stockBefore ➡ stockAfter
        assertEquals(
            "  1:00 AM,   9 tablets ➡ 4 tablets\nTest (5)",
            formatter.formatSimulatedReminder(SimulatedReminder(ScheduledReminder(medicine, reminder, instant), 9.0, 4.0))
                .toString()
        )

        // Variable amount with active stock → no expected stock text
        val variableReminder = reminder.copy(variableAmount = true)
        assertEquals(
            "  1:00 AM\nTest (5)",
            formatter.formatSimulatedReminder(
                SimulatedReminder(ScheduledReminder(medicine, variableReminder, instant), 9.0, 9.0)
            ).toString()
        )

        // Out-of-stock reminder type → no expected stock text (getAmountOrStockString shows medicine amount instead)
        val outOfStockReminder =
            Reminder.default().copy(outOfStockReminderType = Reminder.OutOfStockReminderType.ONCE)
        assertEquals(
            "  1:00 AM,   9 tablets\nTest",
            formatter.formatSimulatedReminder(
                SimulatedReminder(ScheduledReminder(medicine, outOfStockReminder, instant), 9.0, 9.0)
            ).toString()
        )

        // Expiration date reminder type → no expected stock text
        val expirationReminder =
            Reminder.default().copy(expirationReminderType = Reminder.ExpirationReminderType.DAILY)
        medicine = medicine.copy(expirationDate = LocalDate.of(2026, 6, 7))
        assertEquals(
            "  1:00 AM, 6/7/26\nTest",
            formatter.formatSimulatedReminder(
                SimulatedReminder(ScheduledReminder(medicine, expirationReminder, instant), 9.0, 9.0)
            ).toString()
        )
    }
}
