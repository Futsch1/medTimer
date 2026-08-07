package com.futsch1.medtimer

import android.content.Context
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.di.Dispatcher
import com.futsch1.medtimer.core.common.di.MedTimerDispatchers
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.PendingSnooze
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.getShowReminderNotificationIntent
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutostartService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderEventRepository: ReminderEventRepository,
    private val persistentDataDataSource: PersistentDataDataSource,
    private val preferencesDataSource: PreferencesDataSource,
    @param:Dispatcher(MedTimerDispatchers.Default) private val backgroundDispatcher: CoroutineDispatcher
) {
    suspend fun restoreNotifications() = withContext(backgroundDispatcher) {
        Log.i(LogTags.AUTOSTART, "Restore notifications")

        val pendingLocationSnoozes = persistentDataDataSource.getPendingLocationSnoozes()

        val reminderEventList: List<ReminderEvent> = reminderEventRepository.getLastDays(1)
            .filter { it.status == ReminderEvent.ReminderStatus.RAISED && !isLocationSnoozePending(it, pendingLocationSnoozes) }
        val notificationGroups = groupReminderEventsForRestore(
            reminderEventList,
            preferencesDataSource.preferences.value.combineNotifications
        )
        for (reminderEvents in notificationGroups) {
            val reminderIds = reminderEvents.map { it.reminderId }
            val reminderEventIds = reminderEvents.map { it.reminderEventId }
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromArrays(
                    reminderIds,
                    reminderEventIds,
                    reminderEvents.first().remindedTimestamp,
                    -1
                )

            Log.i(LogTags.AUTOSTART, "Restoring reminder event: $scheduledReminderNotificationData")
            val intent = getShowReminderNotificationIntent(context, scheduledReminderNotificationData)
            context.sendBroadcast(intent)
        }
    }

    private fun isLocationSnoozePending(
        reminderEvent: ReminderEvent,
        pendingLocationSnoozes: List<PendingSnooze>
    ): Boolean {
        return pendingLocationSnoozes.any { it.reminderEventIds.contains(reminderEvent.reminderEventId) }
    }
}

internal fun groupReminderEventsForRestore(
    reminderEvents: List<ReminderEvent>,
    combineNotifications: Boolean
): List<List<ReminderEvent>> = if (combineNotifications) {
    reminderEvents.groupBy { it.remindedTimestamp.epochSecond }.values.toList()
} else {
    reminderEvents.map { listOf(it) }
}
