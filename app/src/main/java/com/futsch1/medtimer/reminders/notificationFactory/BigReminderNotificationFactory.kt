package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import android.widget.RemoteViews
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class BigReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
) : ReminderNotificationFactory(
    context,
    notificationId,
    remindTime,
    medicine,
    reminder,
    reminderEvent
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