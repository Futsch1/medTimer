package com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences

import android.content.Context
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class ReminderDataStoreTest {

    private val context: Context = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val timeFormatter: TimeFormatter = mock()
    private val coroutineScope = TestScope()

    private lateinit var dataStore: ReminderDataStore

    @Before
    fun setup() {
        dataStore = ReminderDataStore(
            modelData = Reminder.default(),
            context = context,
            reminderRepository = reminderRepository,
            timeFormatter = timeFormatter,
            coroutineScope = coroutineScope
        )
    }

    @Test
    fun `putString cycle_start_date does not throw when stringToLocalDate returns null`() {
        whenever(timeFormatter.stringToLocalDate("bad-date")).thenReturn(null)

        dataStore.putString("cycle_start_date", "bad-date")

        // modelData unchanged
        assert(dataStore.modelData.cycleStartDay == LocalDate.EPOCH)
    }

    @Test
    fun `putString cycle_start_date does not throw when value is null`() {
        dataStore.putString("cycle_start_date", null)

        assert(dataStore.modelData.cycleStartDay == LocalDate.EPOCH)
    }

    @Test
    fun `putString period_start_date does not throw when stringToLocalDate returns null`() {
        whenever(timeFormatter.stringToLocalDate("bad-date")).thenReturn(null)

        dataStore.putString("period_start_date", "bad-date")

        assert(dataStore.modelData.periodStart == LocalDate.EPOCH)
    }

    @Test
    fun `putString period_end_date does not throw when stringToLocalDate returns null`() {
        whenever(timeFormatter.stringToLocalDate("bad-date")).thenReturn(null)

        dataStore.putString("period_end_date", "bad-date")

        assert(dataStore.modelData.periodEnd == LocalDate.EPOCH)
    }

    @Test
    fun `putString interval_daily_start_time does not throw when value is null`() {
        dataStore.putString("interval_daily_start_time", null)
    }

    @Test
    fun `putString interval_daily_start_time does not throw when timeStringToMinutes returns -1`() {
        whenever(timeFormatter.timeStringToMinutes("bad-time")).thenReturn(-1)

        dataStore.putString("interval_daily_start_time", "bad-time")
    }

    @Test
    fun `putString interval_daily_end_time does not throw when value is null`() {
        dataStore.putString("interval_daily_end_time", null)
    }

    @Test
    fun `putString interval_daily_end_time does not throw when timeStringToMinutes returns -1`() {
        whenever(timeFormatter.timeStringToMinutes("bad-time")).thenReturn(-1)

        dataStore.putString("interval_daily_end_time", "bad-time")
    }

    @Test
    fun `putString cycle_start_date updates modelData when parsing succeeds`() {
        val date = LocalDate.of(2024, 6, 15)
        whenever(timeFormatter.stringToLocalDate("2024-06-15")).thenReturn(date)

        dataStore.putString("cycle_start_date", "2024-06-15")

        assert(dataStore.modelData.cycleStartDay == date)
    }
}
