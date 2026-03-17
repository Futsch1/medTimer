package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_SNOOZE_ACTION
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AndroidEntryPoint
class RemoteInputReceiver : BroadcastReceiver() {
    @Inject
    lateinit var remoteInputReceiverService: RemoteInputReceiverService

    @Inject
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
                REMOTE_INPUT_VARIABLE_AMOUNT_ACTION -> applicationScope.launch { variableAmount(results, reminderNotificationData) }
            }
        }
    }

    private fun snooze(context: Context, results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val snoozeTime = results.getCharSequence(EXTRA_SNOOZE_TIME)?.toString()
        snoozeTime?.toIntOrNull()?.toDuration(DurationUnit.MINUTES)
            ?.let { ReminderProcessorBroadcastReceiver.requestSnooze(context, reminderNotificationData, it) }
    }

    private suspend fun variableAmount(results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val amountsByReminderEventId = extractAmountsFromBundle(results)
        remoteInputReceiverService.handleVariableAmount(amountsByReminderEventId, reminderNotificationData)
    }

    private fun extractAmountsFromBundle(results: Bundle): Map<Int, String> {
        return results.keySet()
            .filter { it.startsWith("amount_") }
            .associate { key ->
                val reminderEventId = key.removePrefix("amount_").toInt()
                val amount = results.getCharSequence(key)?.toString()
                reminderEventId to amount
            }
            .filterValues { !it.isNullOrBlank() }
            .mapValues { it.value!! }
    }
}