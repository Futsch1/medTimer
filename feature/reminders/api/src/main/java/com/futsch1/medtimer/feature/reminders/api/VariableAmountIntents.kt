package com.futsch1.medtimer.feature.reminders.api

import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.api.notificationData.writeTo

fun getVariableAmountActivityIntent(context: Context, reminderNotificationData: ReminderNotificationData): Intent {
    val intent = Intent(ActivityCodes.VARIABLE_AMOUNT_ACTIVITY)
    intent.setClassName(context, "com.futsch1.medtimer.MainActivity")
    reminderNotificationData.writeTo(intent)
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return intent
}
