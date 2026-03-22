package com.futsch1.medtimer

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.futsch1.medtimer.ActivityCodes.CUSTOM_SNOOZE_ACTIVITY
import com.futsch1.medtimer.ActivityCodes.VARIABLE_AMOUNT_ACTIVITY
import com.futsch1.medtimer.overview.customSnoozeDialog
import com.futsch1.medtimer.overview.variableAmountDialog
import com.futsch1.medtimer.reminders.NotificationProcessor
import com.futsch1.medtimer.reminders.ReminderContext

fun dispatch(activity: AppCompatActivity, notificationProcessor: NotificationProcessor, reminderContext: ReminderContext, intent: Intent) {
    Log.d(LogTags.MAIN, "Dispatch intent: ${intent.action}")
    if (intent.action == VARIABLE_AMOUNT_ACTIVITY) {
        variableAmountDialog(activity, intent, notificationProcessor, reminderContext)
    }
    if (intent.action == CUSTOM_SNOOZE_ACTIVITY) {
        customSnoozeDialog(activity, intent)
    }
}