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
    val reminder: Reminder,
    val reminderEvent: ReminderEvent
) : ReminderNotificationFactory(
    context,
    notificationId,
    medicine.medicine
) {
    val views: RemoteViews = RemoteViews(context.packageName, R.layout.notification)

    override fun build() {
        views.setTextViewText(
            R.id.notificationTitle,
            context.getString(R.string.notification_title)
        )

        views.setOnClickPendingIntent(
            R.id.takenButton,
            getTakenPendingIntent(reminderEvent.reminderEventId, reminder)
        )
        builder.setCustomBigContentView(views)

    }
}