package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ActivityCodes.EXTRA_SNOOZE_TIME
import com.futsch1.medtimer.R
import com.futsch1.medtimer.model.DismissNotificationAction
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.RemoteInputReceiver
import com.futsch1.medtimer.reminders.getCustomSnoozeActionIntent
import com.futsch1.medtimer.reminders.getLocationSnoozeIntent
import com.futsch1.medtimer.reminders.getSkippedActionIntent
import com.futsch1.medtimer.reminders.getSnoozeIntent
import com.futsch1.medtimer.reminders.getTakenActionIntent
import com.futsch1.medtimer.reminders.getVariableAmountActivityIntent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class NotificationIntentBuilder @AssistedInject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    @Assisted val reminderNotification: ReminderNotification
) {
    @AssistedFactory
    fun interface Factory {
        fun create(reminderNotification: ReminderNotification): NotificationIntentBuilder
    }

    val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotification.reminderNotificationData)

    val pendingSnooze = getSnoozePendingIntent()
    val actionSnoozeRemoteInput = getSnoozeActionRemoteInput()
    val pendingLocationSnooze = getLocationSnoozePendingIntent()

    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()
    val actionTaken = getTakenActionRemoteInput()

    val pendingDismiss = getDismissPendingIntent()

    private fun getTakenPendingIntent(): PendingIntent {
        val anyAskForAmount = reminderNotification.reminderNotificationParts.any { it.reminderEvent.askForAmount }

        return if (anyAskForAmount) {
            val notifyTaken = getVariableAmountActivityIntent(context, reminderNotification.reminderNotificationData)
            PendingIntent.getActivity(
                context,
                reminderNotification.reminderNotificationData.notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            val notifyTaken = getTakenActionIntent(context, processedNotificationData)
            PendingIntent.getBroadcast(
                context,
                reminderNotification.reminderNotificationData.notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
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
        val snoozeDuration = preferencesDataSource.preferences.value.snoozeDuration

        return if (snoozeDuration.inWholeMinutes < 0) {
            val snooze = getCustomSnoozeActionIntent(
                context, reminderNotification.reminderNotificationData
            )
            PendingIntent.getActivity(context, reminderNotification.reminderNotificationData.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE)
        } else {
            val snooze = getSnoozeIntent(
                context, reminderNotification.reminderNotificationData, snoozeDuration
            )
            PendingIntent.getBroadcast(
                context,
                reminderNotification.reminderNotificationData.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private fun getLocationSnoozePendingIntent(): PendingIntent? {
        if (!preferencesDataSource.preferences.value.locationBasedSnooze || preferencesDataSource.preferences.value.homeLocation == null) {
            return null
        }

        val snooze = getLocationSnoozeIntent(context, reminderNotification.reminderNotificationData)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSnoozeActionRemoteInput(): NotificationCompat.Action? {
        if (preferencesDataSource.preferences.value.snoozeDuration.inWholeSeconds > 0) {
            return null
        }
        val resultIntent = Intent(context, RemoteInputReceiver::class.java).apply {
            action = ActivityCodes.REMOTE_INPUT_SNOOZE_ACTION
        }
        reminderNotification.reminderNotificationData.toIntent(resultIntent)

        val resultPendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderNotification.reminderNotificationData.notificationId,
                resultIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        val remoteInput = RemoteInput.Builder(EXTRA_SNOOZE_TIME).setLabel(context.getString(R.string.snooze_duration)).build()
        val action = NotificationCompat.Action.Builder(
            R.drawable.hourglass_split, context.getString(R.string.snooze), resultPendingIntent
        ).addRemoteInput(remoteInput).build()

        return action
    }

    private fun getTakenActionRemoteInput(): NotificationCompat.Action? {
        if (reminderNotification.reminderNotificationParts.none { it.reminder.variableAmount }) {
            return null
        }
        val resultIntent = Intent(context, RemoteInputReceiver::class.java).apply {
            action = ActivityCodes.REMOTE_INPUT_VARIABLE_AMOUNT_ACTION
        }
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

        for (reminderNotificationPart in reminderNotification.reminderNotificationParts.reversed()) {
            if (!reminderNotificationPart.reminder.variableAmount) {
                continue
            }
            val remoteInput =
                RemoteInput.Builder("amount_${reminderNotificationPart.reminderEvent.reminderEventId}")
                    .setLabel("${context.getString(R.string.dosage)} ${reminderNotificationPart.medicine.name}").build()
            action.addRemoteInput(remoteInput)
        }

        return action.build()
    }

    private fun getDismissPendingIntent(): PendingIntent {
        return when (preferencesDataSource.preferences.value.dismissNotificationAction) {
            DismissNotificationAction.SKIP -> {
                pendingSkipped
            }

            DismissNotificationAction.SNOOZE -> {
                pendingSnooze
            }

            else -> {
                pendingTaken
            }
        }
    }

}
