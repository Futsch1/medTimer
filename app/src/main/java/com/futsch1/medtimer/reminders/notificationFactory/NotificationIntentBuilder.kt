package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class NotificationIntentBuilder(val context: Context, val reminderNotification: ReminderNotification) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotification.reminderNotificationData)

    val pendingSnooze = getSnoozePendingIntent()
    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()
    val pendingDismiss = getDismissPendingIntent()

    private fun getTakenPendingIntent(): PendingIntent {

        val notifyTaken = ReminderProcessor.getTakenActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context, reminderNotification.notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSkippedPendingIntent(): PendingIntent {
        val notifySkipped = ReminderProcessor.getSkippedActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context, reminderNotification.notificationId, notifySkipped, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSnoozePendingIntent(): PendingIntent {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()

        fun getSnoozeCustomTimeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getCustomSnoozeActionIntent(
                context, reminderNotification.reminderNotificationData
            )
            return PendingIntent.getActivity(
                context, reminderNotification.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getStandardSnoozeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getSnoozeIntent(
                context, reminderNotification.reminderNotificationData, snoozeTime
            )
            return PendingIntent.getBroadcast(
                context, reminderNotification.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return if (snoozeTime == -1) {
            getSnoozeCustomTimeIntent()
        } else {
            getStandardSnoozeIntent()
        }
    }

    private fun getDismissPendingIntent(): PendingIntent {
        return when (defaultSharedPreferences.getString("dismiss_notification_action", "0")) {
            "0" -> {
                pendingSkipped
            }

            "1" -> {
                pendingSnooze
            }

            else -> {
                pendingTaken
            }
        }
    }

}