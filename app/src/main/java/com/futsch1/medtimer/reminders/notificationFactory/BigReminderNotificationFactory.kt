package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.widget.RemoteViews
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

class BigReminderNotificationFactory(
    context: Context,
    reminderNotificationData: ReminderNotificationData
) : ReminderNotificationFactory(
    context,
    reminderNotificationData
) {
    val views: RemoteViews = RemoteViews(context.packageName, R.layout.notification)

    override fun build() {
        views.setTextViewText(
            R.id.notificationTitle,
            notificationStrings.notificationString
        )

        views.setOnClickPendingIntent(R.id.takenButton, intents.pendingTaken)
        views.setOnClickPendingIntent(R.id.skippedButton, intents.pendingSkipped)
        views.setOnClickPendingIntent(R.id.snoozeButton, intents.pendingSnooze)
        val isAnyOutOfStock = reminderNotificationData.notificationReminderEvents.any { it.medicine.medicine.isOutOfStock }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (isAnyOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setCustomBigContentView(views)
        builder.setContentText(notificationStrings.baseString)

        buildActions()
    }
}