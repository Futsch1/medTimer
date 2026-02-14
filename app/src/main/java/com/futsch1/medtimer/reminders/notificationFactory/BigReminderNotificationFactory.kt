package com.futsch1.medtimer.reminders.notificationFactory

import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class BigReminderNotificationFactory(
    reminderContext: ReminderContext,
    reminderNotification: ReminderNotification
) : ReminderNotificationFactory(
    reminderContext,
    reminderNotification
) {
    val views: RemoteViews = RemoteViews(reminderContext.packageName, R.layout.notification)

    override fun build() {
        views.setTextViewText(
            R.id.notificationTitle,
            notificationStrings.notificationString
        )

        views.setOnClickPendingIntent(R.id.takenButton, intents.pendingTaken)
        views.setOnClickPendingIntent(R.id.skippedButton, intents.pendingSkipped)
        views.setOnClickPendingIntent(R.id.snoozeButton, intents.pendingSnooze)
        val isAnyOutOfStock = reminderNotification.reminderNotificationParts.any { it.medicine.isOutOfStock }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (isAnyOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder.setCustomBigContentView(views)
        builder.setContentText(notificationStrings.baseString)

        buildActions()
    }
}