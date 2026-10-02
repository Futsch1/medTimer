package com.futsch1.medtimer.feature.ui.overview

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tapping Taken on a notification that asks for the amount can open several amount dialogs for the same event.
 * Only the first confirmation may record the intake; later dialogs must neither take the dose again nor undo it.
 */
class VariableAmountHandlerTest {

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val handler = VariableAmountHandler(reminderEventRepository, mock<ReminderRepository>(), mock<MedicineRepository>(), commandBus, StandardTestDispatcher())

    private fun event(status: ReminderEvent.ReminderStatus) = ReminderEvent.default().copy(
        reminderEventId = EVENT_ID,
        medicineName = "Vitamin X 500 mg",
        amount = "1",
        status = status,
        stockHandled = status == ReminderEvent.ReminderStatus.TAKEN,
    )

    @Test
    fun `should record the intake with the entered amount when the dose is still open`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.RAISED))

        handler.confirmAmount(EVENT_ID, "2")

        val captor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).update(captor.capture())
        assertEquals("2", captor.firstValue.amount)
        verify(commandBus).markReminderEvents(listOf(EVENT_ID), ReminderEvent.ReminderStatus.TAKEN)
    }

    @Test
    fun `should not take the dose again when a second dialog is confirmed`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.TAKEN))

        handler.confirmAmount(EVENT_ID, "2")

        verify(reminderEventRepository, never()).update(any())
        verify(commandBus, never()).markReminderEvents(any(), any())
    }

    @Test
    fun `should not undo a recorded intake when a second dialog is cancelled`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.TAKEN))

        handler.cancelAmount(EVENT_ID)

        verify(reminderEventRepository, never()).update(any())
    }

    @Test
    fun `should touch the open dose when its dialog is cancelled`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.RAISED))

        handler.cancelAmount(EVENT_ID)

        val captor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository).update(captor.capture())
        assertEquals(ReminderEvent.ReminderStatus.RAISED, captor.firstValue.status)
    }

    private companion object {
        const val EVENT_ID = 11
    }
}
