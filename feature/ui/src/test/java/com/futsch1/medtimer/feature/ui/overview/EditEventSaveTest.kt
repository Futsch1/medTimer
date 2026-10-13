package com.futsch1.medtimer.feature.ui.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * The edit sheet saves in onDismiss and is removed right after, which clears its ViewModel.
 * The save has to complete anyway, or edits such as a changed taken time are lost (#1524).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditEventSaveTest {

    private val dispatcher = StandardTestDispatcher()
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val timeFormatter: TimeFormatter = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        whenever(timeFormatter.minutesToTimeString(any())).thenReturn("")
        whenever(timeFormatter.localDateToString(any())).thenReturn("")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should save the edited event even when the dialog is cleared right after dismissing`() = runTest(dispatcher) {
        val event = ReminderEvent.default().copy(
            reminderEventId = 7,
            medicineName = "Vitamin X 500 mg",
            status = ReminderEvent.ReminderStatus.TAKEN,
            processedTimestamp = Instant.ofEpochSecond(1_790_000_000L),
        )
        whenever(reminderEventRepository.getFlow(7)).thenReturn(flowOf(event))
        val viewModel = EditEventViewModel(
            SavedStateHandle(mapOf(EditEventViewModel.ARG_REMINDER_EVENT_ID to 7)),
            reminderEventRepository,
            mock<ReminderRepository>(),
            mock<ReminderCommandBus>(),
            timeFormatter,
            CoroutineScope(dispatcher),
        )
        advanceUntilIdle()

        viewModel.updateEvent()
        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        verify(reminderEventRepository).update(any())
    }
}
