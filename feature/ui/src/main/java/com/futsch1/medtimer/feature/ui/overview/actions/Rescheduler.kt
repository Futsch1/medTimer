package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.common.helpers.TimeHelper
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.ui.overview.model.PastReminderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** The events to move to a time the user has yet to pick; all of them land on the day of the first one. */
class RescheduleRequest(
    val scheduledReminders: List<ScheduledReminder>,
    val pastReminderEvents: List<PastReminderEvent>,
) {
    init {
        require(scheduledReminders.isNotEmpty() || pastReminderEvents.isNotEmpty())
    }

    val remindedInstant: Instant = Instant.ofEpochSecond(
        pastReminderEvents.firstOrNull()?.timestamp ?: scheduledReminders.first().timestamp.epochSecond
    )

    val initialTime: LocalTime get() = remindedInstant.atZone(ZoneId.systemDefault()).toLocalTime()
}

class Rescheduler @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderEventCreator: ReminderEventCreator,
    private val preferencesDataSource: PreferencesDataSource,
    private val commandBus: ReminderCommandBus,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    suspend fun reschedule(request: RescheduleRequest, time: LocalTime) {
        val newReminderTime = TimeHelper.changeTimeMinutes(request.remindedInstant, time.hour * 60 + time.minute)

        val pendingReminderEvents = request.scheduledReminders.map { scheduledReminder ->
            reminderEventCreator.resolvePending(scheduledReminder, newReminderTime.epochSecond)
        } + request.pastReminderEvents.map { pastReminderEvent ->
            pastReminderEvent.reminderEvent.copy(remindedTimestamp = newReminderTime)
        }
        val newReminderEvents = reminderEventRepository.upsertMany(pendingReminderEvents)

        applicationScope.launch { commandBus.scheduleNextNotification() }

        if (preferencesDataSource.preferences.value.combineNotifications) {
            requestShowReminders(newReminderEvents, newReminderTime)
        } else {
            newReminderEvents.forEach {
                requestShowReminders(listOf(it), newReminderTime)
            }
        }
    }

    private fun requestShowReminders(reminderEvents: List<ReminderEvent>, remindInstant: Instant) {
        val reminderNotificationData = ReminderNotificationData.fromArrays(
            reminderEvents.map { it.reminderId },
            reminderEvents.map { it.reminderEventId },
            remindInstant
        )
        applicationScope.launch { commandBus.showReminderNotification(reminderNotificationData) }
    }
}
