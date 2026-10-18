package com.futsch1.medtimer.feature.ui.overview

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderNotificationTapHandlerTest {

    private val dispatcher = StandardTestDispatcher()
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val overviewDayRequests = OverviewDayRequests()
    private val handler = ReminderNotificationTapHandler(reminderEventRepository, commandBus, overviewDayRequests, dispatcher)

    private val yesterday = LocalDate.now().minusDays(1)
    private val lastWeek = LocalDate.now().minusDays(7)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Instant =
        day.atTime(LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant()

    private fun event(id: Int, reminded: Instant, status: ReminderEvent.ReminderStatus = ReminderEvent.ReminderStatus.RAISED) =
        ReminderEvent.default().copy(reminderEventId = id, medicineName = "Medicine A", remindedTimestamp = reminded, status = status)

    @Test
    fun `should open the scheduled day of a reminder snoozed past midnight`() = runTest(dispatcher) {
        whenever(reminderEventRepository.fetch(1)).thenReturn(event(1, at(yesterday, 23, 30)))

        handler.handle(NOTIFICATION_ID, listOf(1), at(lastWeek, 8))

        assertEquals(yesterday, overviewDayRequests.requests.first().day)
        verify(commandBus, never()).removeRemindersFromNotification(any(), any())
    }

    @Test
    fun `should open the earliest day of the reminders in a group`() = runTest(dispatcher) {
        whenever(reminderEventRepository.fetch(1)).thenReturn(event(1, at(LocalDate.now(), 0, 5)))
        whenever(reminderEventRepository.fetch(2)).thenReturn(event(2, at(yesterday, 23, 55)))

        handler.handle(NOTIFICATION_ID, listOf(1, 2), at(lastWeek, 8))

        assertEquals(yesterday, overviewDayRequests.requests.first().day)
    }

    @Test
    fun `should use the remaining reminders and drop deleted ones from the notification`() = runTest(dispatcher) {
        whenever(reminderEventRepository.fetch(1)).thenReturn(null)
        whenever(reminderEventRepository.fetch(2)).thenReturn(event(2, at(lastWeek, 9), ReminderEvent.ReminderStatus.DELETED))
        whenever(reminderEventRepository.fetch(3)).thenReturn(event(3, at(yesterday, 10)))

        handler.handle(NOTIFICATION_ID, listOf(1, 2, 3), at(lastWeek, 8))

        val request = overviewDayRequests.requests.first()
        assertEquals(yesterday, request.day)
        assertEquals(listOf(3), request.reminderEvents.map { it.reminderEventId })
        verify(commandBus).removeRemindersFromNotification(NOTIFICATION_ID, listOf(1, 2))
    }

    @Test
    fun `should open the fallback day and remove the notification when all reminders are deleted`() = runTest(dispatcher) {
        whenever(reminderEventRepository.fetch(1)).thenReturn(null)
        whenever(reminderEventRepository.fetch(2)).thenReturn(event(2, at(yesterday, 9), ReminderEvent.ReminderStatus.DELETED))

        handler.handle(NOTIFICATION_ID, listOf(1, 2), at(lastWeek, 8))

        val request = overviewDayRequests.requests.first()
        assertEquals(lastWeek, request.day)
        assertEquals(emptyList<ReminderEvent>(), request.reminderEvents)
        verify(commandBus).removeRemindersFromNotification(NOTIFICATION_ID, listOf(1, 2))
    }

    private companion object {
        const val NOTIFICATION_ID = 42
    }
}
