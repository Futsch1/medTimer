package com.futsch1.medtimer.reminders

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_SNOOZE_ACTION
import com.futsch1.medtimer.ActivityCodes.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RemoteInputReceiver(val dispatcher: CoroutineDispatcher = Dispatchers.IO) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        if (results != null) {
            val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)
            when (intent.action) {
                REMOTE_INPUT_SNOOZE_ACTION -> snooze(context, results, reminderNotificationData)
                REMOTE_INPUT_VARIABLE_AMOUNT_ACTION -> variableAmount(context, results, reminderNotificationData)
            }
        }
    }

    private fun snooze(context: Context, results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val snoozeTime = results.getCharSequence("snooze_time")
        val snoozeTimeInt = snoozeTime.toString().toIntOrNull() ?: 10
        confirmNotification(context, reminderNotificationData.notificationId)
        ReminderProcessorBroadcastReceiver.requestSnooze(context, reminderNotificationData, snoozeTimeInt)
    }

    private fun variableAmount(context: Context, results: Bundle, reminderNotificationData: ReminderNotificationData) {
        val reminderContext = ReminderContext(context)
        ProcessLifecycleOwner.get().lifecycleScope.launch(dispatcher) {
            val medicineRepository = MedicineRepository(context.applicationContext as Application)
            val reminderNotification = ReminderNotification.fromReminderNotificationData(
                reminderContext,
                reminderNotificationData
            ) ?: return@launch

            val reminderEvents = mutableListOf<ReminderEvent>()

            for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
                if (!reminderNotificationPart.reminder.variableAmount) {
                    reminderEvents.add(reminderNotificationPart.reminderEvent)
                } else {
                    val amount = results.getCharSequence("amount_${reminderNotificationPart.reminderEvent.reminderEventId}")
                    if (amount != null) {
                        Log.d(LogTags.REMINDER, "Setting variable amount to $amount of reID ${reminderNotificationPart.reminderEvent.reminderEventId}")

                        reminderEvents.add(reminderNotificationPart.reminderEvent)
                        reminderNotificationPart.reminderEvent.amount = amount.toString()
                        medicineRepository.updateReminderEvent(reminderNotificationPart.reminderEvent)
                        confirmNotification(context, reminderNotificationData.notificationId)
                    }
                }
            }

            NotificationProcessor(reminderContext).setReminderEventStatus(
                ReminderEvent.ReminderStatus.TAKEN,
                reminderEvents,
            )
        }
    }

    private fun confirmNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                val builder = NotificationCompat.Builder(context, notification.notification.channelId)
                builder.setSmallIcon(notification.notification.smallIcon.resId).setContentInfo("")
                builder.setTimeoutAfter(4000)
                builder.setExtras(notification.notification.extras)
                notificationManager.notify(notificationId, builder.build())
            }
        }
    }
}