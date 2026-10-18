package com.futsch1.medtimer.feature.ui.overview

import android.content.Intent
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Handles a tap on a reminder notification: requests the overview day of its reminders and drops reminders that were
 * deleted in the meantime from the notification.
 */
class ReminderNotificationTapHandler @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val commandBus: ReminderCommandBus,
    private val overviewDayRequests: OverviewDayRequests,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun handle(intent: Intent) {
        val extras = intent.extras ?: return
        val reminderNotificationData = extras.toReminderNotificationData()
        val remindedInstant = Instant.ofEpochSecond(extras.getLong(ActivityCodes.EXTRA_REMINDED_INSTANT, reminderNotificationData.remindInstant.epochSecond))
        handle(reminderNotificationData.notificationId, reminderNotificationData.reminderEventIds, remindedInstant)
    }

    suspend fun handle(notificationId: Int, reminderEventIds: List<Int>, fallbackRemindedInstant: Instant) {
        val existingEvents = withContext(ioDispatcher) {
            reminderEventIds.mapNotNull { id -> reminderEventRepository.fetch(id)?.takeIf { it.status != ReminderEvent.ReminderStatus.DELETED } }
        }
        val remindedInstant = existingEvents.minOfOrNull { it.remindedTimestamp } ?: fallbackRemindedInstant
        overviewDayRequests.request(remindedInstant.toLocalDate())

        val deletedEventIds = reminderEventIds - existingEvents.map { it.reminderEventId }.toSet()
        if (deletedEventIds.isNotEmpty() && notificationId != -1) {
            commandBus.removeRemindersFromNotification(notificationId, deletedEventIds)
        }
    }

    private fun Instant.toLocalDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()
}
