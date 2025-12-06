package com.futsch1.medtimer.overview

import android.content.Intent
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.getSnoozeIntent
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

fun customSnoozeDialog(activity: AppCompatActivity, intent: Intent) {
    val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)

    if (!reminderNotificationData.valid) {
        return
    }
    // Cancel a potential repeat alarm
    NotificationProcessor.cancelPendingAlarms(activity, reminderNotificationData.reminderEventIds[0])

    DialogHelper(activity)
        .title(R.string.snooze_duration)
        .hint(R.string.minutes_string)
        .initialText("")
        .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
        .textSink { snoozeTime: String? ->
            val snoozeTimeInt = snoozeTime?.toIntOrNull()
            if (snoozeTimeInt != null) {
                val snooze = getSnoozeIntent(
                    activity,
                    reminderNotificationData,
                    snoozeTimeInt
                )
                activity.sendBroadcast(snooze, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
            }
        }
        .show()

}
