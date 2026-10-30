package com.futsch1.medtimer.processortests

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderTime
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.AlarmProcessor
import com.futsch1.medtimer.feature.reminders.NotificationProcessor
import com.futsch1.medtimer.feature.reminders.ReminderEditProcessor
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.scheduling.withSnapshotFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderEditProcessorTest {
    private val zone: ZoneId = ZoneId.of("Z")
    private val today: LocalDate = LocalDate.EPOCH.plusDays(5)
    private val now: Instant = at(8, 30)

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val notificationProcessor: NotificationProcessor = mock()
    private val alarmProcessor: AlarmProcessor = mock()
    private val persistentDataDataSource: PersistentDataDataSource = mock()
    private val preferencesDataSource: PreferencesDataSource = mock {
        on { preferences } doReturn MutableStateFlow(UserPreferences.default())
    }
    private val commandBus: ReminderCommandBus = mock()
    private val timeAccess = object : TimeAccess {
        override fun systemZone(): ZoneId = zone
        override fun localDate(): LocalDate = today
        override fun now(): Instant = now
    }

    private val processor = ReminderEditProcessor(
        reminderEventRepository, notificationProcessor, alarmProcessor, persistentDataDataSource, preferencesDataSource, timeAccess, commandBus
    )

    private val reminderA = Reminder.default().copy(id = 1, medicineRelId = 1, amount = "1", time = ReminderTime(480))
    private val reminderB = Reminder.default().copy(id = 2, medicineRelId = 1, amount = "1", time = ReminderTime(480))
    private val medicine = Medicine.default().copy(id = 1, name = "Medicine A", reminders = listOf(reminderA, reminderB))

    private val eventA = event(11, reminderA)
    private val eventB = event(12, reminderB)

    private fun at(hour: Int, minute: Int): Instant = today.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun event(id: Int, reminder: Reminder): ReminderEvent =
        ReminderEvent.default().copy(reminderEventId = id, reminderId = reminder.id, remindedTimestamp = at(8, 0), notificationId = 5)
            .withSnapshotFields(medicine, reminder)

    private fun withOpenEvents(vararg events: ReminderEvent) {
        reminderEventRepository.stub {
            onBlocking { getOpenFrom(any(), any()) } doReturn emptyList()
            for (event in events) {
                onBlocking { getOpenFrom(eq(event.reminderId), any()) } doReturn listOf(event)
            }
            onBlocking { getForScheduling(any()) } doReturn events.toList()
        }
    }

    private fun edited(newReminderA: Reminder?): Medicine =
        medicine.copy(reminders = listOfNotNull(newReminderA, reminderB))

    @Test
    fun firstEmissionOnlySetsBaseline() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))

        verifyNoInteractions(reminderEventRepository, notificationProcessor, alarmProcessor)
        verify(commandBus).scheduleNextNotification()
    }

    @Test
    fun unchangedMedicineListDoesNothing() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(medicine.copy()))

        verifyNoInteractions(reminderEventRepository, notificationProcessor, alarmProcessor)
    }

    @Test
    fun stockOnlyChangeDoesNothing() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine.copy(amount = 10.0)))
        processor.onMedicinesChanged(listOf(medicine.copy(amount = 9.0)))

        verifyNoInteractions(reminderEventRepository, notificationProcessor, alarmProcessor)
    }

    @Test
    fun dropCancelsAlarmsRemovesFromNotificationAndDeletes() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(reminderA.copy(time = ReminderTime(540)))))

        val order = inOrder(alarmProcessor, notificationProcessor, reminderEventRepository, commandBus)
        order.verify(alarmProcessor).cancelPendingReminderNotifications(11)
        order.verify(notificationProcessor).removeRemindersFromNotification(5, listOf(11), true)
        order.verify(reminderEventRepository).delete(eventA)
        // Reconciliation runs before the scheduler raises the dose at its new time
        order.verify(commandBus).scheduleNextNotification()
    }

    @Test
    fun dropKeepsOtherEventsInCombinedNotification() = runBlocking {
        withOpenEvents(eventA, eventB)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(reminderA.copy(time = ReminderTime(540)))))

        verify(notificationProcessor).removeRemindersFromNotification(5, listOf(11), true)
        verify(reminderEventRepository, never()).delete(eventB)
        verify(alarmProcessor, never()).cancelPendingReminderNotifications(12)
    }

    @Test
    fun dropRemovesPendingLocationSnooze() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(null)))

        verify(persistentDataDataSource).removePendingLocationSnoozesForReminderEventIds(listOf(11))
        verify(reminderEventRepository).delete(eventA)
    }

    @Test
    fun refreshRebuildsPostedNotificationSilently() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(reminderA.copy(amount = "2"))))

        val order = inOrder(reminderEventRepository, notificationProcessor)
        order.verify(reminderEventRepository).update(eventA.copy(amount = "2"))
        order.verify(notificationProcessor).refreshNotification(5)
        verify(reminderEventRepository, never()).delete(any())
        verify(alarmProcessor, never()).cancelPendingReminderNotifications(any<Int>())
    }

    @Test
    fun retimeRebuildsNotificationSilentlyAndKeepsRepeat() = runBlocking {
        withOpenEvents(eventA)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(reminderA.copy(time = ReminderTime(495)))))

        verify(reminderEventRepository).update(eventA.copy(remindedTimestamp = at(8, 15)))
        verify(notificationProcessor).refreshNotification(5)
        // Repeat and snooze alarms of the dose keep running
        verify(alarmProcessor, never()).cancelPendingReminderNotifications(any<Int>())
        verify(notificationProcessor, never()).removeRemindersFromNotification(any<Int>(), any(), any())
    }

    @Test
    fun retimeKeepsSnoozeAlarm() = runBlocking {
        val snoozed = eventA.copy(notificationId = -1)
        withOpenEvents(snoozed)
        processor.onMedicinesChanged(listOf(medicine))
        processor.onMedicinesChanged(listOf(edited(reminderA.copy(time = ReminderTime(500)))))

        verify(reminderEventRepository).update(snoozed.copy(remindedTimestamp = at(8, 20)))
        verify(alarmProcessor, never()).cancelPendingReminderNotifications(any<Int>())
        verify(persistentDataDataSource, never()).removePendingLocationSnoozesForReminderEventIds(any())
    }
}
