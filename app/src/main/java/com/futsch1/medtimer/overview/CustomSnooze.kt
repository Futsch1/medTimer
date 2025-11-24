package com.futsch1.medtimer.overview

import android.content.Intent
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.notifications.Notification

fun customSnoozeDialog(activity: AppCompatActivity, intent: Intent) {
    val notification = Notification.fromBundle(intent.extras!!, null)

    if (!notification.valid) {
        return
    }
    // Cancel a potential repeat alarm
    NotificationProcessor.cancelPendingAlarms(activity, notification.notificationReminderEvents[0].reminderEvent.reminderEventId)

    DialogHelper(activity)
        .title(R.string.snooze_duration)
        .hint(R.string.minutes_string)
        .initialText("")
        .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
        .textSink { snoozeTime: String? ->
            val snoozeTimeInt = snoozeTime?.toIntOrNull()
            if (snoozeTimeInt != null) {
                val snooze = ReminderProcessor.getSnoozeIntent(
                    activity,
                    notification,
                    snoozeTimeInt
                )
                activity.sendBroadcast(snooze, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
            }
        }
        .show()

}
