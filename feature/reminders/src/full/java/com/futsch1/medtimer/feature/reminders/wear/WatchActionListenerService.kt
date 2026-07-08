package com.futsch1.medtimer.feature.reminders.wear

import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives an action requested from the watch app and drives it through the exact same entry
 * points a notification action button uses, so nothing about stock handling or scheduling is
 * duplicated - see [ReminderProcessorBroadcastReceiver.requestReminderAction] and siblings.
 */
@AndroidEntryPoint
class WatchActionListenerService : WearableListenerService() {
    @Inject
    lateinit var reminderEventRepository: ReminderEventRepository

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    private val gson = Gson()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearProtocol.ACTION_MESSAGE_PATH) return

        val watchAction = runCatching {
            gson.fromJson(String(messageEvent.data, Charsets.UTF_8), WatchAction::class.java)
        }.getOrNull() ?: return

        scope.launch { handle(watchAction) }
    }

    private suspend fun handle(watchAction: WatchAction) {
        val reminderEvent = reminderEventRepository.fetch(watchAction.reminderEventId) ?: run {
            Log.w(LogTags.REMINDER, "Watch action for unknown reID ${watchAction.reminderEventId}")
            return
        }
        val reminder = reminderRepository.fetch(reminderEvent.reminderId)

        when (watchAction.action) {
            WearProtocol.ACTION_TAKEN ->
                ReminderProcessorBroadcastReceiver.requestReminderAction(this, reminder, reminderEvent, true)

            WearProtocol.ACTION_SKIPPED ->
                ReminderProcessorBroadcastReceiver.requestReminderAction(this, reminder, reminderEvent, false)

            WearProtocol.ACTION_SNOOZE -> {
                val snoozeDuration = preferencesDataSource.preferences.value.snoozeDuration
                ReminderProcessorBroadcastReceiver.requestSnooze(
                    this, ReminderNotificationData.fromReminderEvent(reminderEvent), snoozeDuration
                )
            }

            WearProtocol.ACTION_SNOOZE_HOME ->
                ReminderProcessorBroadcastReceiver.requestLocationSnooze(
                    this, ReminderNotificationData.fromReminderEvent(reminderEvent)
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
