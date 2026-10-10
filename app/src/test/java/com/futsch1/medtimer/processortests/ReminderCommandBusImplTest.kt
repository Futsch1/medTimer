package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.LocationSnoozeProcessor
import com.futsch1.medtimer.feature.reminders.NotificationProcessor
import com.futsch1.medtimer.feature.reminders.RefillProcessor
import com.futsch1.medtimer.feature.reminders.ReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.ScheduleNextReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.ShowReminderNotificationProcessor
import com.futsch1.medtimer.feature.reminders.SnoozeProcessor
import com.futsch1.medtimer.feature.reminders.StockHandlingProcessor
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.command.ReminderCommandBusImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.Instant
import kotlin.test.assertEquals

/**
 * Guards the reminder scheduling chain: every command that can disturb alarm state
 * must leave the next-reminder alarm (slot 0) recomputed afterwards.
 *
 * Regression test for the command-bus merge dropping the reschedule after
 * showReminder that was added in 991084b0 ("Schedule the next reminder after
 * showing one to make sure that no rescheduled reminder is raised twice").
 */
class ReminderCommandBusImplTest {

    private val notificationProcessor: NotificationProcessor = mock()
    private val snoozeProcessor: SnoozeProcessor = mock()
    private val refillProcessor: RefillProcessor = mock()
    private val reminderNotificationProcessor: ReminderNotificationProcessor = mock()
    private val scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor = mock()
    private val showReminderNotificationProcessor: ShowReminderNotificationProcessor = mock()
    private val stockHandlingProcessor: StockHandlingProcessor = mock()
    private val locationSnoozeProcessor: LocationSnoozeProcessor = mock()

    private val commandBus = ReminderCommandBusImpl(
        notificationProcessor,
        snoozeProcessor,
        refillProcessor,
        reminderNotificationProcessor,
        scheduleNextReminderNotificationProcessor,
        showReminderNotificationProcessor,
        stockHandlingProcessor,
        locationSnoozeProcessor
    )

    private val data = ReminderNotificationData(
        remindInstant = Instant.ofEpochSecond(1000),
        reminderIds = listOf(1),
        reminderEventIds = listOf(11)
    )

    @Test
    fun showReminderNotificationReschedulesNextReminder() {
        runBlocking {
            commandBus.showReminderNotification(data)

            verify(showReminderNotificationProcessor).showReminder(data)
            // Show reminder may have rescheduled the next due reminder, so the
            // next-reminder alarm must be recomputed afterwards
            verify(scheduleNextReminderNotificationProcessor).scheduleNextReminder()
        }
    }

    /**
     * Marking events re-posts the notification without the processed events. Concurrent calls (one per
     * event when several are marked from the overview) each read the original notification and the last
     * one to re-post wins, leaving an already taken event in the notification (#1589).
     */
    @Test
    fun markReminderEventsProcessesOneCallAtATime() {
        runBlocking {
            val gate = CompletableDeferred<Unit>()
            var inside = 0
            var maxInside = 0
            notificationProcessor.stub {
                onBlocking { processReminderEventsInNotification(any(), any()) } doSuspendableAnswer {
                    inside++
                    maxInside = maxOf(maxInside, inside)
                    gate.await()
                    inside--
                    Unit
                }
            }

            val first = launch { commandBus.markReminderEvents(listOf(11), ReminderEvent.ReminderStatus.TAKEN) }
            val second = launch { commandBus.markReminderEvents(listOf(12), ReminderEvent.ReminderStatus.TAKEN) }
            yield()
            yield()
            gate.complete(Unit)
            joinAll(first, second)

            assertEquals(1, maxInside)
        }
    }

    @Test
    fun markReminderEventsReschedulesNextReminder() {
        runBlocking {
            commandBus.markReminderEvents(listOf(11), ReminderEvent.ReminderStatus.TAKEN)

            verify(scheduleNextReminderNotificationProcessor).scheduleNextReminder()
        }
    }

    @Test
    fun scheduleNextNotificationDoesNotShowReminder() {
        runBlocking {
            commandBus.scheduleNextNotification()

            verify(scheduleNextReminderNotificationProcessor).scheduleNextReminder()
            verify(showReminderNotificationProcessor, never()).showReminder(data)
        }
    }
}
