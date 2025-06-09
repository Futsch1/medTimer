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
            getNotificationString()
        )

        views.setOnClickPendingIntent(R.id.takenButton, pendingTaken)
        views.setOnClickPendingIntent(R.id.skippedButton, pendingSkipped)
        views.setOnClickPendingIntent(R.id.snoozeButton, pendingSnooze)
        if (hasSameTimeReminders) {
            views.setViewVisibility(R.id.allTakenButton, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.allTakenButton, pendingAllTaken)
        }
        views.setTextViewCompoundDrawablesRelative(
            R.id.notificationTitle,
            if (medicine.medicine.isOutOfStock) R.drawable.exclamation_triangle_fill else 0,
            0,
            0,
            0
        )

        builder.setCustomBigContentView(views)
        builder.setContentText(baseString)

        buildActions()
    }

    override fun showOutOfStockIcon(): Boolean {
        return false
    }

}