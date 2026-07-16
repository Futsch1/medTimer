package com.futsch1.medtimer.feature.reminders.notificationData

import android.app.PendingIntent
import android.content.Context
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.api.notificationData.writeTo
import com.futsch1.medtimer.feature.reminders.getReminderAction

fun ReminderNotificationData.toPendingIntent(context: Context): PendingIntent {
    val reminderIntent = getReminderAction(context)
    writeTo(reminderIntent)
    return PendingIntent.getBroadcast(
        context,
        reminderEventIds[0],
        reminderIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
