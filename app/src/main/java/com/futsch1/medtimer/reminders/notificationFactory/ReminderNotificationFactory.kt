package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor


fun getReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
): ReminderNotificationFactory {
    val defaultPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    return if (defaultPreferences.getBoolean("big_notifications", false)) {
        BigReminderNotificationFactory(
            context,
            notificationId,
            remindTime,
            medicine,
            reminder,
            reminderEvent
        )
    } else {
        SimpleReminderNotificationFactory(
            context,
            notificationId,
            remindTime,
            medicine,
            reminder,
            reminderEvent
        )
    }
}

abstract class ReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    medicine: Medicine,
) : NotificationFactory(context, notificationId, medicine) {
    val defaultSharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    init {
        val contentIntent: PendingIntent? = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule)
            .setContentTitle(context.getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
    }

    abstract fun build()

    override fun create(): Notification {
        build()
        return builder.build()
    }

    fun getTakenPendingIntent(
        reminderEventId: Int,
        reminder: Reminder
    ): PendingIntent? {
        return if (reminder.variableAmount) {
            val notifyTaken = ReminderProcessor.getVariableAmountActionIntent(
                context,
                reminderEventId,
                reminder.amount
            )
            PendingIntent.getActivity(
                context,
                notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val notifyTaken = ReminderProcessor.getTakenActionIntent(context, reminderEventId)
            PendingIntent.getBroadcast(
                context,
                notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}