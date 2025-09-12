package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.futsch1.medtimer.R

class BigReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
) : ReminderNotificationFactory(
    context,
    notificationId,
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
        if (hasSameTimeReminders) {
            views.setViewVisibility(R.id.allTakenButton, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.allTakenButton, intents.pendingAllTaken)
            views.setTextViewText(R.id.allTakenButton, context.getString(R.string.all_taken, remindTime))
        }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (medicine.medicine.isOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setCustomBigContentView(views)
        builder.setContentText(notificationStrings.baseString)

        buildActions()
    }
}