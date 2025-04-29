package com.futsch1.medtimer.overview

import android.content.Intent
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.helpers.DialogHelper
import com.futsch1.medtimer.reminders.NotificationAction
import com.futsch1.medtimer.reminders.ReminderProcessor

fun customSnoozeDialog(activity: AppCompatActivity, intent: Intent) {
    val reminderId: Int = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
    val reminderEventId: Int = intent.getIntExtra(EXTRA_REMINDER_EVENT_ID, -1)
    val notificationId: Int = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

    // Cancel a potential repeat alarm
    NotificationAction.cancelPendingAlarms(activity, reminderEventId)

    DialogHelper(activity)
        .title(R.string.snooze_duration)
        .hint(R.string.minutes_string)
        .initialText("")
        .inputType(InputType.TYPE_NUMBER_FLAG_SIGNED)
        .textSink { snoozeTime: String? ->
            val snoozeTimeInt = snoozeTime?.toIntOrNull()
            if (snoozeTimeInt != null) {
                val snooze = ReminderProcessor.getSnoozeIntent(
                    activity,
                    reminderId,
                    reminderEventId,
                    notificationId,
                    snoozeTimeInt
                )
                activity.sendBroadcast(snooze, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
            }
        }
        .show()

}
