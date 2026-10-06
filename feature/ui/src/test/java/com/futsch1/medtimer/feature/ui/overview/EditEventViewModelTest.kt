package com.futsch1.medtimer.feature.ui.overview

import androidx.lifecycle.SavedStateHandle
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals

/**
 * Coverage for the stock consequences of editing a reminder event: the amount and the status of an
 * event decide how much stock the dose takes, so editing them has to correct the medicine stock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val timeFormatter: TimeFormatter = mock()

    private val eventFlow = MutableStateFlow<ReminderEvent?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(reminderEventRepository.getFlow(EVENT_ID)).thenReturn(eventFlow)
        runBlocking {
            whenever(reminderRepository.fetch(REMINDER_ID)).thenReturn(
                Reminder.default().copy(id = REMINDER_ID, medicineRelId = MEDICINE_ID)
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should take the difference from stock when the amount of a taken event is raised`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = 10.0, stockAfter = 9.0)
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(8.0)

        val viewModel = createViewModel()
        viewModel.setAmount("2")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals("2", updatedEvent.amount)
        assertEquals(ReminderEvent.ReminderStatus.TAKEN, updatedEvent.status)
        assertEquals(true, updatedEvent.stockHandled)
        assertEquals(8.0, updatedEvent.stockAfter)
        verify(commandBus).processStockHandling(eq(1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should hand back the difference when the amount of a taken event is lowered`() = runTest {
        eventFlow.value = event(amount = "2", stockBefore = 10.0, stockAfter = 8.0)
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(9.0)

        val viewModel = createViewModel()
        viewModel.setAmount("1")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(9.0, updatedEvent.stockAfter)
        assertEquals(true, updatedEvent.stockHandled)
        verify(commandBus).processStockHandling(eq(-1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should leave the stock untouched when the amount of a taken event is kept`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = 10.0, stockAfter = 9.0)

        val viewModel = createViewModel()
        viewModel.setNotes("Note")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(9.0, updatedEvent.stockAfter)
        assertEquals(true, updatedEvent.stockHandled)
        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    @Test
    fun `should take the difference from stock only once when the same event is edited twice`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = 10.0, stockAfter = 9.0)
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(8.0)

        val viewModel = createViewModel()
        viewModel.setAmount("2")
        viewModel.updateEvent()
        viewModel.updateEvent()

        val updatedEvents = capturedUpdates(2)
        assertEquals(8.0, updatedEvents.first().stockAfter)
        assertEquals(8.0, updatedEvents.last().stockAfter)
        verify(commandBus, times(1)).processStockHandling(eq(1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should hand back the taken amount when a taken event is skipped`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = 10.0, stockAfter = 9.0)
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(10.0)

        val viewModel = createViewModel()
        viewModel.status = ReminderEvent.ReminderStatus.SKIPPED
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(ReminderEvent.ReminderStatus.SKIPPED, updatedEvent.status)
        assertEquals(false, updatedEvent.stockHandled)
        assertEquals(10.0, updatedEvent.stockAfter)
        verify(commandBus).processStockHandling(eq(-1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should take the amount from stock when a skipped event is taken`() = runTest {
        eventFlow.value = event(
            amount = "2",
            status = ReminderEvent.ReminderStatus.SKIPPED,
            stockHandled = false,
            stockBefore = 10.0,
            stockAfter = 10.0
        )
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(8.0)

        val viewModel = createViewModel()
        viewModel.status = ReminderEvent.ReminderStatus.TAKEN
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(ReminderEvent.ReminderStatus.TAKEN, updatedEvent.status)
        assertEquals(true, updatedEvent.stockHandled)
        assertEquals(8.0, updatedEvent.stockAfter)
        verify(commandBus).processStockHandling(eq(2.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should leave the stock untouched when an event that never took stock is edited`() = runTest {
        eventFlow.value = event(
            amount = "1",
            status = ReminderEvent.ReminderStatus.RAISED,
            stockHandled = false,
            stockBefore = 10.0,
            stockAfter = 10.0
        )

        val viewModel = createViewModel()
        viewModel.setAmount("2")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(false, updatedEvent.stockHandled)
        assertEquals(10.0, updatedEvent.stockAfter)
        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    @Test
    fun `should leave the stock untouched when the medicine does not manage stock`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = -1.0, stockAfter = -1.0)

        val viewModel = createViewModel()
        viewModel.setAmount("2")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(-1.0, updatedEvent.stockAfter)
        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    @Test
    fun `should leave the stock untouched when the event has no reminder`() = runTest {
        eventFlow.value = event(amount = "1", stockBefore = 10.0, stockAfter = 9.0).copy(reminderId = -1)

        val viewModel = createViewModel()
        viewModel.setAmount("2")
        viewModel.updateEvent()

        val updatedEvent = capturedUpdate()
        assertEquals(9.0, updatedEvent.stockAfter)
        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    private fun createViewModel() = EditEventViewModel(
        SavedStateHandle(mapOf(EditEventViewModel.ARG_REMINDER_EVENT_ID to EVENT_ID)),
        reminderEventRepository,
        reminderRepository,
        commandBus,
        timeFormatter
    )

    private suspend fun capturedUpdate(): ReminderEvent = capturedUpdates(1).first()

    private suspend fun capturedUpdates(count: Int): List<ReminderEvent> {
        val captor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository, times(count)).update(captor.capture())
        return captor.allValues
    }

    private fun event(
        amount: String,
        status: ReminderEvent.ReminderStatus = ReminderEvent.ReminderStatus.TAKEN,
        stockHandled: Boolean = true,
        stockBefore: Double,
        stockAfter: Double
    ) = ReminderEvent.default().copy(
        reminderEventId = EVENT_ID,
        reminderId = REMINDER_ID,
        medicineName = "Medicine A",
        amount = amount,
        status = status,
        remindedTimestamp = Instant.ofEpochSecond(REMINDED_EPOCH_SECONDS),
        processedTimestamp = Instant.ofEpochSecond(PROCESSED_EPOCH_SECONDS),
        stockHandled = stockHandled,
        stockBefore = stockBefore,
        stockAfter = stockAfter,
        stockUnit = "pills"
    )

    private companion object {
        const val EVENT_ID = 1
        const val REMINDER_ID = 2
        const val MEDICINE_ID = 5
        const val REMINDED_EPOCH_SECONDS = 720 * 60L
        const val PROCESSED_EPOCH_SECONDS = 730 * 60L
    }
}
