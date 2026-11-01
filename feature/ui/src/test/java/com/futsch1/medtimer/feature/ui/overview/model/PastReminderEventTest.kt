package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Coverage for the stock the overview shows for a reminder event: a dose that has been taken shows
 * the stock it took, a dose that is still pending shows the stock it will take.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PastReminderEventTest {

    private val preferencesDataSource: PreferencesDataSource = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val timeAccess: TimeAccess = mock()

    private val currentDay = LocalDate.ofEpochDay(CURRENT_EPOCH_DAY)

    @Before
    fun setUp() {
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(UserPreferences.default()))
        whenever(persistentDataDataSource.getPendingLocationSnoozes()).thenReturn(emptyList())
        whenever(timeAccess.systemZone()).thenReturn(UTC)
        whenever(timeAccess.localDate()).thenReturn(currentDay)
        whenever(timeAccess.now()).thenReturn(onDay(currentDay))
    }

    @Test
    fun `should show the stock a pending dose of the current day takes`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 10.0)

        assertEquals(StockChange(10.0, 8.0, "pills"), shownStock(event))
    }

    @Test
    fun `should show the stock a pending dose of a later day takes`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay.plusDays(1), stock = 10.0)

        assertEquals(StockChange(10.0, 8.0, "pills"), shownStock(event))
    }

    @Test
    fun `should not take more stock than the medicine has`() {
        val event = event(amount = "12", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 10.0)

        assertEquals(StockChange(10.0, 0.0, "pills"), shownStock(event))
    }

    @Test
    fun `should show the empty stock of a pending dose`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 0.0)

        assertEquals(StockChange(0.0, 0.0, "pills"), shownStock(event))
    }

    @Test
    fun `should show no stock for a pending dose of a past day`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay.minusDays(1), stock = 10.0)

        assertNull(shownStock(event))
    }

    @Test
    fun `should show the stock a taken dose took`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.TAKEN, day = currentDay, stock = 10.0)
            .copy(stockAfter = 8.0)

        assertEquals(StockChange(10.0, 8.0, "pills"), shownStock(event))
    }

    @Test
    fun `should show no stock for a dose that took none`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.SKIPPED, day = currentDay, stock = 10.0)

        assertNull(shownStock(event))
    }

    @Test
    fun `should show no stock for a pending dose that asks for its amount`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 10.0)
            .copy(askForAmount = true)

        assertNull(shownStock(event))
    }

    @Test
    fun `should show no stock for a pending medicine without stock management`() {
        val event = event(amount = "2", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = -1.0)

        assertNull(shownStock(event))
    }

    @Test
    fun `should show no stock for a pending out of stock reminder`() {
        val event = event(amount = "10 pills", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 10.0)
            .copy(reminderType = ReminderType.OUT_OF_STOCK)

        assertNull(shownStock(event))
    }

    @Test
    fun `should show no stock for a pending dose without an amount`() {
        val event = event(amount = "?", status = ReminderEvent.ReminderStatus.RAISED, day = currentDay, stock = 10.0)

        assertNull(shownStock(event))
    }

    private fun shownStock(event: ReminderEvent) =
        PastReminderEvent(preferencesDataSource, persistentDataDataSource, timeAccess, event).content.stock

    private fun event(
        amount: String,
        status: ReminderEvent.ReminderStatus,
        day: LocalDate,
        stock: Double
    ) = ReminderEvent.default().copy(
        reminderEventId = 1,
        reminderId = 1,
        medicineName = "Medicine A",
        amount = amount,
        status = status,
        remindedTimestamp = onDay(day),
        processedTimestamp = Instant.EPOCH,
        stockBefore = stock,
        stockAfter = stock,
        stockUnit = "pills"
    )

    private fun onDay(day: LocalDate): Instant = day.atTime(8, 0).atZone(UTC).toInstant()

    private companion object {
        const val CURRENT_EPOCH_DAY = 20000L
        val UTC: ZoneId = ZoneId.of("UTC")
    }
}
