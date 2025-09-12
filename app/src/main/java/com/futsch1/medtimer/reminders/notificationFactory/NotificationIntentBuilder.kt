package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor

class NotificationIntentBuilder(val context: Context, val notificationId: Int, val reminderEvent: ReminderEvent, val reminder: Reminder) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val pendingSnooze = getSnoozePendingIntent()
    val pendingSkipped = getSkippedPendingIntent()
    val pendingTaken = getTakenPendingIntent()
    val pendingAllTaken = getAllTakenPendingIntent()
    val pendingDismiss = getDismissPendingIntent()

    private fun getTakenPendingIntent(): PendingIntent {
        return if (reminder.variableAmount) {
            val notifyTaken = ReminderProcessor.getVariableAmountActionIntent(
                context, reminderEvent.reminderEventId, reminder.amount
            )
            PendingIntent.getActivity(
                context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEvent.reminderEventId)
            PendingIntent.getBroadcast(
                context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private fun getAllTakenPendingIntent(): PendingIntent {
        val notifyTaken = ReminderProcessor.getAllTakenActionIntent(context, reminderEvent.reminderEventId)
        return PendingIntent.getBroadcast(
            context, notificationId, notifyTaken, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSkippedPendingIntent(): PendingIntent {
        val notifySkipped = ReminderProcessor.getSkippedActionIntent(context, reminderEvent.reminderEventId)
        return PendingIntent.getBroadcast(
            context, notificationId, notifySkipped, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getSnoozePendingIntent(): PendingIntent {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()

        fun getSnoozeCustomTimeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getCustomSnoozeActionIntent(
                context, reminder.reminderId, reminderEvent.reminderEventId, notificationId
            )
            return PendingIntent.getActivity(
                context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getStandardSnoozeIntent(): PendingIntent {
            val snooze = ReminderProcessor.getSnoozeIntent(
                context, reminder.reminderId, reminderEvent.reminderEventId, notificationId, snoozeTime
            )
            return PendingIntent.getBroadcast(
                context, notificationId, snooze, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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