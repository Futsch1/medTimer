package com.futsch1.medtimer.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification


fun getReminderNotificationFactory(
    reminderContext: ReminderContext,
    reminderNotification: ReminderNotification
): NotificationFactory {
    return if (reminderNotification.isOutOfStockNotification()) {
        OutOfStockNotificationFactory(
            reminderContext,
            reminderNotification
        )
    } else if (reminderNotification.isExpirationDateNotification()) {
        ExpirationDateNotificationFactory(
            reminderContext,
            reminderNotification
        )
    } else {
        if (reminderContext.preferences.getBoolean("big_notifications", false)) {
            BigReminderNotificationFactory(
                reminderContext, reminderNotification
            )
        } else {
            SimpleReminderNotificationFactory(
                reminderContext, reminderNotification
            )
        }
    }
}

abstract class ReminderNotificationFactory(
    reminderContext: ReminderContext,
    val reminderNotification: ReminderNotification
) : NotificationFactory(
    reminderContext,
    reminderNotification.reminderNotificationData.notificationId,
    reminderNotification.reminderNotificationParts.map { it.medicine.medicine }) {
    val defaultSharedPreferences: SharedPreferences = reminderContext.preferences

    val intents = NotificationIntentBuilder(
        reminderContext, reminderNotification
    )
    val notificationStrings =
        NotificationStringBuilder(
            reminderContext,
            reminderNotification
        )

    init {
        val contentIntent: PendingIntent = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule)
        builder.setContentTitle(reminderContext.getString(R.string.notification_title))
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setCategory(android.app.Notification.CATEGORY_REMINDER)
        builder.setContentIntent(contentIntent)

        builder.setDeleteIntent(intents.pendingDismiss)

        // Set group key to reminder notification time string so that same time reminders are grouped
        builder.setGroup(
            reminderNotification.reminderNotificationData.remindInstant.epochSecond.toString()
        )

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
        val pendingIntent = reminderContext.getPendingIntentActivity(
            0,
            ReminderAlarmActivity.getIntent(
                reminderContext, reminderNotification.reminderNotificationData
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
            R.drawable.x_circle, reminderContext.getString(R.string.skipped), intents.pendingSkipped
        )
    }

    private fun addSnoozeAction() {
        val action = intents.actionSnoozeRemoteInput
        if (action != null) {
            builder.addAction(action)
        } else {
            builder.addAction(
                R.drawable.hourglass_split, reminderContext.getString(R.string.snooze), intents.pendingSnooze
            )
        }
    }

    private fun addTakenAction() {
        val action = intents.actionTaken
        if (action != null) {
            builder.addAction(action)
        } else {
            builder.addAction(
                R.drawable.check2_circle, reminderContext.getString(R.string.taken), intents.pendingTaken
            )
        }
    }

}