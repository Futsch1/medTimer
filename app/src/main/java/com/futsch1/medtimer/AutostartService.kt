package com.futsch1.medtimer

import android.content.Context
import android.util.Log
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.getShowReminderNotificationIntent
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutostartService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderEventRepository: ReminderEventRepository,
    @param:Dispatcher(MedTimerDispatchers.Default) private val backgroundDispatcher: CoroutineDispatcher
) {
    suspend fun restoreNotifications() = withContext(backgroundDispatcher) {
        Log.i(LogTags.AUTOSTART, "Restore notifications")

        val reminderEventList: List<ReminderEvent> = reminderEventRepository.getLastDays(1)
            .filter { it.status == ReminderEvent.ReminderStatus.RAISED }
        val notificationsMap: Map<Long, List<ReminderEvent>> = reminderEventList.groupBy { it.remindedTimestamp.epochSecond }
        for (notificationEntry in notificationsMap) {
            val reminderIds = notificationEntry.value.map { it.reminderId }
            val reminderEventIds = notificationEntry.value.map { it.reminderEventId }.toMutableList()
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromArrays(
                    reminderIds,
                    reminderEventIds,
                    Instant.ofEpochSecond(notificationEntry.key),
                    -1
                )
            Log.i(LogTags.AUTOSTART, "Restoring reminder event: $scheduledReminderNotificationData")
            val intent = getShowReminderNotificationIntent(context, scheduledReminderNotificationData)
            context.sendBroadcast(intent)
        }
    }
}