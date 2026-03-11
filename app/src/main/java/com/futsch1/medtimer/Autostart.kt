package com.futsch1.medtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.futsch1.medtimer.LogTags.AUTOSTART
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestScheduleNextNotification
import com.futsch1.medtimer.reminders.getShowReminderNotificationIntent
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.function.Predicate
import java.util.stream.Collectors

class Autostart(
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.MY_PACKAGE_REPLACED")) {
            if (hasRestored) {
                return
            }
            hasRestored = true

            // TODO: once Hilt is set upped, inject a global coroutine scope
            CoroutineScope(SupervisorJob() + backgroundDispatcher).launch {
                restoreNotifications(context)
            }
            Log.i(AUTOSTART, "Requesting reschedule")
            requestScheduleNextNotification(context)
        }
    }

    companion object {
        var hasRestored = false

        @SuppressWarnings("kotlin:S5320") // Sending to local receiver is safe
        suspend fun restoreNotifications(context: Context) {
            Log.i(AUTOSTART, "Restore notifications")
            withContext(Dispatchers.Default) {
                val repo = MedicineRepository(context)
                val reminderEventList: List<ReminderEvent> = repo.getLastDaysReminderEvents(1).stream()
                    .filter((Predicate { reminderEvent: ReminderEvent -> reminderEvent.status == ReminderEvent.ReminderStatus.RAISED })).collect(
                        Collectors.toUnmodifiableList()
                    )
                val notificationsMap: Map<Long, List<ReminderEvent>> = reminderEventList.groupBy { it.remindedTimestamp }
                for (notificationEntry in notificationsMap) {
                    val reminderIds = notificationEntry.value.stream().mapToInt { it.reminderId }.toArray()
                    val reminderEventIds = notificationEntry.value.stream().mapToInt { it.reminderEventId }.toArray()
                    val scheduledReminderNotificationData =
                        ReminderNotificationData.fromArrays(
                            reminderIds,
                            reminderEventIds,
                            Instant.ofEpochSecond(notificationEntry.key),
                            -1
                        )
                    Log.i(AUTOSTART, "Restoring reminder event: $scheduledReminderNotificationData")
                    val intent = getShowReminderNotificationIntent(context, scheduledReminderNotificationData)
                    context.sendBroadcast(intent)
                }
            }
        }
    }
}
