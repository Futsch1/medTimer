package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.RemoteInputReceiver
import com.futsch1.medtimer.reminders.getCustomSnoozeActionIntent
import com.futsch1.medtimer.reminders.getSkippedActionIntent
import com.futsch1.medtimer.reminders.getSnoozeIntent
import com.futsch1.medtimer.reminders.getTakenActionIntent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class NotificationIntentBuilder(val context: Context, val reminderNotification: ReminderNotification) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotification.reminderNotificationData)

    val pendingSnooze = getSnoozePendingIntent()
    val actionSnoozeRemoteInput = getSnoozeActionRemoteInput()
    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()
    val actionTaken = getTakenActionRemoteInput()

    val pendingDismiss = getDismissPendingIntent()

    private fun getTakenPendingIntent(): PendingIntent {
        val notifyTaken = getTakenActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifyTaken,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSkippedPendingIntent(): PendingIntent {
        val notifySkipped = getSkippedActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifySkipped,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSnoozePendingIntent(): PendingIntent {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()

        fun getSnoozeCustomTimeIntent(): PendingIntent {
            val snooze = getCustomSnoozeActionIntent(
                context, reminderNotification.reminderNotificationData
            )
            return PendingIntent.getActivity(
                context, reminderNotification.reminderNotificationData.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getStandardSnoozeIntent(): PendingIntent {
            val snooze = getSnoozeIntent(
                context, reminderNotification.reminderNotificationData, snoozeTime
            )
            return PendingIntent.getBroadcast(
                context, reminderNotification.reminderNotificationData.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return if (snoozeTime == -1) {
            getSnoozeCustomTimeIntent()
        } else {
            getStandardSnoozeIntent()
        }
    }

    private fun getSnoozeActionRemoteInput(): NotificationCompat.Action? {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()
        if (snoozeTime != -1) {
            return null
        }
        val resultIntent = Intent(context, RemoteInputReceiver::class.java)
        resultIntent.action = ActivityCodes.REMOTE_INPUT_SNOOZE_ACTION
        reminderNotification.reminderNotificationData.toIntent(resultIntent)

        val resultPendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderNotification.reminderNotificationData.notificationId,
                resultIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val remoteInput = RemoteInput.Builder("snooze_time").setLabel(context.getString(R.string.snooze_duration)).build()
        val action = NotificationCompat.Action.Builder(
            R.drawable.hourglass_split, context.getString(R.string.snooze), resultPendingIntent
        ).addRemoteInput(remoteInput).build()

        return action
    }

    private fun getTakenActionRemoteInput(): NotificationCompat.Action? {
        if (reminderNotification.reminderNotificationParts.none { it.reminder.variableAmount }) {
            return null
        }
        val resultIntent = Intent(context, RemoteInputReceiver::class.java)
        resultIntent.action = ActivityCodes.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION
        reminderNotification.reminderNotificationData.toIntent(resultIntent)

        val resultPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId + 1000,
            resultIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val action = NotificationCompat.Action.Builder(
            R.drawable.check2_circle, context.getString(R.string.taken), resultPendingIntent
        )

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                continue
            }
            val remoteInput =
                RemoteInput.Builder("amount_${reminderNotificationPart.reminderEvent.reminderEventId}")
                    .setLabel("${context.getString(R.string.dosage)} ${reminderNotificationPart.medicine.medicine.name}").build()
            action.addRemoteInput(remoteInput)
        }

        return action.build()
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