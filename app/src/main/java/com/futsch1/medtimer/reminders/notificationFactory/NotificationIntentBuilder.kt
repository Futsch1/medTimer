package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import com.futsch1.medtimer.model.DismissNotificationAction
import com.futsch1.medtimer.preferences.PreferencesDataSource
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
    val pendingLocationSnooze = getLocationSnoozePendingIntent()

    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()

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
