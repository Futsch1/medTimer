package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.MedicineHelper.getMedicineNameWithStockTextForNotification

class SimpleReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    val remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
) : ReminderNotificationFactory(
    context,
    notificationId,
    medicine,
    reminder,
    reminderEvent
) {
    override fun build() {
        val notificationMessage: String = getNotificationString(remindTime, reminder, medicine)

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }

    private fun getNotificationString(
        remindTime: String?,
        reminder: Reminder,
        medicine: FullMedicine
    ): String {
        val medicineNameString =
            getMedicineNameWithStockTextForNotification(context, medicine.medicine)
        val notificationString =
            "$medicineNameString (${reminder.amount}) ${getInstructions()} - $remindTime"

        val tagNames =
            getTagNames()
        if (tagNames.isEmpty()) {
            return notificationString
        }
        return notificationString + "\n(" + java.lang.String.join(", ", tagNames) + ")"
    }

    private fun buildActions(
    ) {
        val dismissNotificationAction: String? =
            defaultSharedPreferences.getString("dismiss_notification_action", "0")
        val stickyOnLockscreen: Boolean =
            defaultSharedPreferences.getBoolean("sticky_on_lockscreen", false)

        val pendingSnooze: PendingIntent = getSnoozePendingIntent()
        val pendingSkipped = getSkippedPendingIntent()

        val pendingTaken: PendingIntent? =
            getTakenPendingIntent()

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
            builder.setDeleteIntent(pendingSkipped)
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
            builder.setDeleteIntent(pendingSnooze)
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
            builder.setDeleteIntent(pendingTaken)
        }

        // Later than Android 14, make notification ongoing so that it cannot be dismissed from the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && stickyOnLockscreen) {
            builder.setOngoing(true)
        }
    }

}