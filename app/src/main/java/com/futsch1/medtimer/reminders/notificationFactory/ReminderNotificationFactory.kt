package com.futsch1.medtimer.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification


fun getReminderNotificationFactory(
    context: Context,
    reminderNotification: ReminderNotification
): ReminderNotificationFactory {
    val defaultPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return if (defaultPreferences.getBoolean("big_notifications", false)) {
        BigReminderNotificationFactory(
            context, reminderNotification
        )
    } else {
        SimpleReminderNotificationFactory(
            context, reminderNotification
        )
    }
}

abstract class ReminderNotificationFactory(
    context: Context,
    val reminderNotification: ReminderNotification
) : NotificationFactory(
    context,
    reminderNotification.reminderNotificationData.notificationId,
    reminderNotification.reminderNotificationParts.map { it.medicine.medicine }) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val intents = NotificationIntentBuilder(
        context, reminderNotification
    )
    val notificationStrings =
        NotificationStringBuilder(
            context,
            reminderNotification
        )

    init {
        val contentIntent: PendingIntent? = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule).setContentTitle(context.getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setCategory(android.app.Notification.CATEGORY_REMINDER).setContentIntent(contentIntent)

        builder.setDeleteIntent(intents.pendingDismiss)

        // Set group key to reminder notification time string so that same time reminders are grouped
        builder.setGroup(TimeHelper.secondsSinceEpochToTimeString(context, reminderNotification.reminderNotificationData.remindInstant.epochSecond))

        // Later than Android 14, make notification ongoing so that it cannot be dismissed from the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && defaultSharedPreferences.getBoolean(
                "sticky_on_lockscreen", false
            )
        ) {
            builder.setOngoing(true)
        }
        // If shown as alarm, add a full screen intent
        if (reminderNotification.reminderNotificationParts.any { it.medicine.medicine.showNotificationAsAlarm }) {
            addFullScreenIntent()
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun addFullScreenIntent() {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            ReminderAlarmActivity.getIntent(
                context, reminderNotification.reminderNotificationData
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setCategory(android.app.Notification.CATEGORY_ALARM)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setFullScreenIntent(pendingIntent, true)
    }

    abstract fun build()

    override fun create(): android.app.Notification {
        build()
        reminderNotification.reminderNotificationData.toBundle(builder.extras)
        return builder.build()
    }


    fun buildActions(
    ) {
        val dismissNotificationAction: String? = defaultSharedPreferences.getString("dismiss_notification_action", "0")

        when (dismissNotificationAction) {
            "0" -> {
                addTakenAction()
                addSnoozeAction()
            }

            "1" -> {
                addTakenAction()
                addSkippedAction()
            }

            else -> {
                addSkippedAction()
                addSnoozeAction()
            }
        }
    }

    private fun addSkippedAction() {
        builder.addAction(
            R.drawable.x_circle, context.getString(R.string.skipped), intents.pendingSkipped
        )
    }

    private fun addSnoozeAction() {
        builder.addAction(
            R.drawable.hourglass_split, context.getString(R.string.snooze), intents.pendingSnooze
        )
    }

    private fun addTakenAction() {
        builder.addAction(
            R.drawable.check2_circle, context.getString(R.string.taken), intents.pendingTaken
        )
    }

}