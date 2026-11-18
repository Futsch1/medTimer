package com.futsch1.medtimer.feature.ui.overview

import androidx.fragment.app.FragmentActivity
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Tapping Taken on a notification that asks for the amount can open several amount dialogs for the same event.
 * Only the first confirmation may record the intake; later dialogs must neither take the dose again nor undo it.
 * Taken at chains one dialog per dose and takes each confirmed dose at the picked time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VariableAmountHandlerTest {

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val activity: FragmentActivity = mock()
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
        verify(commandBus, never()).markReminderEvents(any(), any(), anyOrNull())
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

    @Test
    fun `should take doses that do not ask for an amount and only prompt for the others`() = runTest {
        val askingEvent = event(ReminderEvent.ReminderStatus.RAISED).copy(askForAmount = true)
        val plainEvent = event(ReminderEvent.ReminderStatus.RAISED).copy(reminderEventId = OTHER_EVENT_ID)
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(askingEvent)
        whenever(reminderEventRepository.fetch(OTHER_EVENT_ID)).thenReturn(plainEvent)

        val toPrompt = handler.takeDosesWithoutAmount(listOf(EVENT_ID, OTHER_EVENT_ID))

        assertEquals(listOf(askingEvent), toPrompt)
        verify(commandBus).markReminderEvents(listOf(OTHER_EVENT_ID), ReminderEvent.ReminderStatus.TAKEN)
    }

    @Test
    fun `should not prompt for doses that are no longer open`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.TAKEN).copy(askForAmount = true))

        val toPrompt = handler.takeDosesWithoutAmount(listOf(EVENT_ID))

        assertEquals(emptyList<ReminderEvent>(), toPrompt)
        verify(commandBus, never()).markReminderEvents(any(), any(), anyOrNull())
    }

    @Test
    fun `should write the amount before taking the dose at the picked time`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.RAISED))

        handler.confirmAmount(EVENT_ID, "2", TAKEN_TIME)

        val inOrder = inOrder(reminderEventRepository, commandBus)
        inOrder.verify(reminderEventRepository).update(event(ReminderEvent.ReminderStatus.RAISED).copy(amount = "2"))
        inOrder.verify(commandBus).markReminderEvents(listOf(EVENT_ID), ReminderEvent.ReminderStatus.TAKEN, TAKEN_TIME)
    }

    @Test
    fun `should take only the doses whose amount is confirmed`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.RAISED))
        whenever(reminderEventRepository.fetch(OTHER_EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.RAISED).copy(reminderEventId = OTHER_EVENT_ID))
        var cancelledRowCreated = false
        val chainHandler = handlerAnswering("2", null, "3")

        chainHandler.takeWithAmounts(
            activity,
            listOf(
                VariableAmountHandler.AmountRequest("Medicine A", "1") { EVENT_ID },
                VariableAmountHandler.AmountRequest("Medicine B", "1") { cancelledRowCreated = true; CANCELLED_EVENT_ID },
                VariableAmountHandler.AmountRequest("Medicine C", "1") { OTHER_EVENT_ID },
            ),
            TAKEN_TIME
        )

        assertFalse(cancelledRowCreated)
        val captor = argumentCaptor<ReminderEvent>()
        verify(reminderEventRepository, times(2)).update(captor.capture())
        assertEquals(listOf(EVENT_ID to "2", OTHER_EVENT_ID to "3"), captor.allValues.map { it.reminderEventId to it.amount })
        verify(commandBus).markReminderEvents(listOf(EVENT_ID), ReminderEvent.ReminderStatus.TAKEN, TAKEN_TIME)
        verify(commandBus).markReminderEvents(listOf(OTHER_EVENT_ID), ReminderEvent.ReminderStatus.TAKEN, TAKEN_TIME)
        verify(reminderEventRepository, never()).fetch(CANCELLED_EVENT_ID)
    }

    @Test
    fun `should not take a requested dose that is no longer open`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.TAKEN))

        handlerAnswering("2").takeWithAmounts(activity, listOf(VariableAmountHandler.AmountRequest("Medicine A", "1") { EVENT_ID }), TAKEN_TIME)

        verify(reminderEventRepository, never()).update(any())
        verify(commandBus, never()).markReminderEvents(any(), any(), anyOrNull())
    }

    @Test
    fun `should take a skipped dose when its amount is confirmed on the overview`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.SKIPPED))

        handlerAnswering("2").takeWithAmounts(activity, listOf(VariableAmountHandler.AmountRequest("Medicine A", "1") { EVENT_ID }), TAKEN_TIME)

        val inOrder = inOrder(reminderEventRepository, commandBus)
        inOrder.verify(reminderEventRepository).update(event(ReminderEvent.ReminderStatus.SKIPPED).copy(amount = "2"))
        inOrder.verify(commandBus).markReminderEvents(listOf(EVENT_ID), ReminderEvent.ReminderStatus.TAKEN, TAKEN_TIME)
    }

    @Test
    fun `should not take a deleted dose on the overview`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.DELETED))

        handlerAnswering("2").takeWithAmounts(activity, listOf(VariableAmountHandler.AmountRequest("Medicine A", "1") { EVENT_ID }), TAKEN_TIME)

        verify(reminderEventRepository, never()).update(any())
        verify(commandBus, never()).markReminderEvents(any(), any(), anyOrNull())
    }

    @Test
    fun `should not take a skipped dose from the notification dialog`() = runTest {
        whenever(reminderEventRepository.fetch(EVENT_ID)).thenReturn(event(ReminderEvent.ReminderStatus.SKIPPED))

        handler.confirmAmount(EVENT_ID, "2")

        verify(reminderEventRepository, never()).update(any())
        verify(commandBus, never()).markReminderEvents(any(), any(), anyOrNull())
    }

    /** The real handler with its dialog replaced by [amounts], answered one per dialog; null stands for Cancel. */
    private suspend fun TestScope.handlerAnswering(vararg amounts: String?): VariableAmountHandler {
        val chainHandler = spy(
            VariableAmountHandler(
                reminderEventRepository, mock<ReminderRepository>(), mock<MedicineRepository>(), commandBus, UnconfinedTestDispatcher(testScheduler)
            )
        )
        doReturn(amounts.first(), *amounts.drop(1).toTypedArray()).whenever(chainHandler).askAmount(any(), any(), any())
        return chainHandler
    }

    private companion object {
        val TAKEN_TIME: Instant = Instant.parse("2026-10-19T06:15:00Z")
        const val CANCELLED_EVENT_ID = 13
        const val OTHER_EVENT_ID = 12
        const val EVENT_ID = 11
    }
}
