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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EditEventViewModelTest {

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val timeFormatter: TimeFormatter = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(timeFormatter.minutesToTimeString(any())).thenReturn("")
        whenever(timeFormatter.localDateToString(any())).thenReturn("")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun takenEvent(amount: String = "1") = ReminderEvent.default().copy(
        reminderEventId = EVENT_ID,
        reminderId = REMINDER_ID,
        medicineName = "Medicine A",
        amount = amount,
        status = ReminderEvent.ReminderStatus.TAKEN,
        remindedTimestamp = Instant.ofEpochSecond(PROCESSED_EPOCH_SECONDS),
        processedTimestamp = Instant.ofEpochSecond(PROCESSED_EPOCH_SECONDS),
        stockHandled = true,
        stockBefore = 30.0,
        stockAfter = 29.0,
    )

    private suspend fun viewModelFor(event: ReminderEvent): EditEventViewModel {
        whenever(reminderEventRepository.getFlow(EVENT_ID)).thenReturn(flowOf(event))
        whenever(reminderRepository.fetch(REMINDER_ID)).thenReturn(Reminder.default().copy(id = REMINDER_ID, medicineRelId = MEDICINE_ID))
        whenever(commandBus.processStockHandling(any(), any(), any())).thenReturn(0.0)
        return EditEventViewModel(
            SavedStateHandle(mapOf(EditEventViewModel.ARG_REMINDER_EVENT_ID to EVENT_ID)),
            reminderEventRepository,
            reminderRepository,
            commandBus,
            timeFormatter,
        )
    }

    private suspend fun savedEvent(): ReminderEvent {
        val captor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).update(captor.capture())
        return captor.firstValue
    }

    @Test
    fun `should deduct the extra amount from stock when the amount of a taken dose is increased`() = runTest {
        val viewModel = viewModelFor(takenEvent(amount = "1"))

        viewModel.setAmount("2")
        viewModel.updateEvent()

        verify(commandBus).processStockHandling(eq(1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
        assertEquals(28.0, savedEvent().stockAfter, 0.0)
    }

    @Test
    fun `should return stock when the amount of a taken dose is decreased`() = runTest {
        val viewModel = viewModelFor(takenEvent(amount = "2"))

        viewModel.setAmount("1")
        viewModel.updateEvent()

        verify(commandBus).processStockHandling(eq(-1.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
    }

    @Test
    fun `should return the whole dose to stock when a taken dose is changed to skipped`() = runTest {
        val viewModel = viewModelFor(takenEvent(amount = "2"))

        viewModel.status = ReminderEvent.ReminderStatus.SKIPPED
        viewModel.updateEvent()

        verify(commandBus).processStockHandling(eq(-2.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
        assertEquals(false, savedEvent().stockHandled)
    }

    @Test
    fun `should deduct the dose from stock when a skipped dose is changed to taken`() = runTest {
        val viewModel = viewModelFor(
            takenEvent(amount = "2").copy(status = ReminderEvent.ReminderStatus.SKIPPED, stockHandled = false, stockBefore = 30.0, stockAfter = 30.0)
        )

        viewModel.status = ReminderEvent.ReminderStatus.TAKEN
        viewModel.updateEvent()

        verify(commandBus).processStockHandling(eq(2.0), eq(MEDICINE_ID), eq(PROCESSED_EPOCH_SECONDS))
        assertEquals(true, savedEvent().stockHandled)
    }

    @Test
    fun `should leave stock untouched when only the notes change`() = runTest {
        val viewModel = viewModelFor(takenEvent())

        viewModel.setNotes("After breakfast")
        viewModel.updateEvent()

        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    @Test
    fun `should leave stock untouched for a manual dose without a reminder`() = runTest {
        val viewModel = viewModelFor(takenEvent(amount = "1").copy(reminderId = -1))
        whenever(reminderRepository.fetch(-1)).thenReturn(null)

        viewModel.setAmount("2")
        viewModel.updateEvent()

        verify(commandBus, never()).processStockHandling(any(), any(), any())
    }

    private companion object {
        const val EVENT_ID = 7
        const val REMINDER_ID = 3
        const val MEDICINE_ID = 5
        const val PROCESSED_EPOCH_SECONDS = 1_790_000_000L
    }
}
