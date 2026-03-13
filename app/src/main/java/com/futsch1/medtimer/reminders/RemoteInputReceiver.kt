package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_SNOOZE_ACTION
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RemoteInputReceiver : BroadcastReceiver() {
    @Inject
    lateinit var remoteInputReceiverService: RemoteInputReceiverService

    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        if (results != null) {
            val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)
            when (intent.action) {
                REMOTE_INPUT_SNOOZE_ACTION -> snooze(context, results, reminderNotificationData)
                REMOTE_INPUT_VARIABLE_AMOUNT_ACTION -> applicationScope.launch { variableAmount(context, results, reminderNotificationData) }
            }
        }
    }

    private fun snooze(context: Context, results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val snoozeTime = results.getCharSequence("snooze_time")
        val snoozeTimeInt = snoozeTime.toString().toIntOrNull() ?: 10
        confirmNotification(context, reminderNotificationData.notificationId)
        ReminderProcessorBroadcastReceiver.requestSnooze(context, reminderNotificationData, snoozeTimeInt)
    }

    private suspend fun variableAmount(context: Context, results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val amountsByReminderEventId = extractAmountsFromBundle(results)
        remoteInputReceiverService.handleVariableAmount(amountsByReminderEventId, reminderNotificationData)
        confirmNotification(context, reminderNotificationData.notificationId)
    }

    private fun extractAmountsFromBundle(results: Bundle): Map<Int, String> {
        return results.keySet()
            .filter { it.startsWith("amount_") }
            .associate { key ->
                val reminderEventId = key.removePrefix("amount_").toInt()
                val amount = results.getCharSequence(key)?.toString() ?: return@associate reminderEventId to null
                reminderEventId to amount
            }
            .filterValues { it != null }
            .mapValues { it.value!! }
    }

    private fun confirmNotification(context: Context, notificationId: Int) {
        notificationManager.cancel(notificationId)
        for (notification in notificationManager.activeNotifications) {
            if (notification.id != notificationId) {
                continue
            }

            val notification = NotificationCompat.Builder(context, notification.notification.channelId).apply {
                setSmallIcon(notification.notification.smallIcon.resId)
                setContentInfo("")
                setTimeoutAfter(4000)
                setExtras(notification.notification.extras)
            }.build()
            notificationManager.notify(notificationId, notification)
        }
    }
}