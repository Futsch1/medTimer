package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SimpleReminderNotificationFactory(
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
    override fun build() {
        val notificationMessage = getNotificationString()

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }

    private fun buildActions(
    ) {
        val dismissNotificationAction: String? =
            defaultSharedPreferences.getString("dismiss_notification_action", "0")

        if (dismissNotificationAction == "0") {
            builder.addAction(
                R.drawable.check2_circle,
                context.getString(R.string.taken),
                pendingTaken
            )
            builder.addAction(
                R.drawable.hourglass_split,
                context.getString(R.string.snooze),
                pendingSnooze
            )
        } else if (dismissNotificationAction == "1") {
            builder.addAction(
                R.drawable.check2_circle,
                context.getString(R.string.taken),
                pendingTaken
            )
            builder.addAction(
                R.drawable.x_circle,
                context.getString(R.string.skipped),
                pendingSkipped
            )
        } else {
            builder.addAction(
                R.drawable.x_circle,
                context.getString(R.string.skipped),
                pendingSkipped
            )
            builder.addAction(
                R.drawable.hourglass_split,
                context.getString(R.string.snooze),
                pendingSnooze
            )
        }
    }

}