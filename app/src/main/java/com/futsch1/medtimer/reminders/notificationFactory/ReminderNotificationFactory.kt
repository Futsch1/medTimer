package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.reminders.ReminderProcessor
import java.util.stream.Collectors


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
    val medicine: FullMedicine,
    val reminder: Reminder,
    val reminderEvent: ReminderEvent
) : NotificationFactory(context, notificationId, medicine.medicine) {
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
    ): PendingIntent? {
        return if (reminder.variableAmount) {
            val notifyTaken = ReminderProcessor.getVariableAmountActionIntent(
                context,
                reminderEvent.reminderEventId,
                reminder.amount
            )
            PendingIntent.getActivity(
                context,
                notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val notifyTaken =
                ReminderProcessor.getTakenActionIntent(context, reminderEvent.reminderEventId)
            PendingIntent.getBroadcast(
                context,
                notificationId,
                notifyTaken,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    fun getInstructions(): String {
        var instructions = reminder.instructions
        if (instructions == null) {
            instructions = ""
        }
        return addLineBreakIfNotEmpty(instructions)
    }

    fun addLineBreakIfNotEmpty(string: String): String {
        return if (string.isEmpty()) {
            string
        } else {
            "$string\n"
        }
    }

    fun getTagNames(): String {
        val tagNames = medicine.tags.stream().map<String?> { t: Tag? -> t!!.name }
            .collect(Collectors.toList())
        return java.lang.String.join(", ", tagNames)
    }

    fun getSkippedPendingIntent(): PendingIntent {
        val notifySkipped =
            ReminderProcessor.getSkippedActionIntent(context, reminderEvent.reminderEventId)
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            notifySkipped,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun getSnoozePendingIntent(): PendingIntent {
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()

        return if (snoozeTime == -1) {
            val snooze = ReminderProcessor.getCustomSnoozeActionIntent(
                context,
                reminder.reminderId,
                reminderEvent.reminderEventId,
                notificationId
            )
            PendingIntent.getActivity(
                context,
                notificationId,
                snooze,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val snooze = ReminderProcessor.getSnoozeIntent(
                context,
                reminder.reminderId,
                reminderEvent.reminderEventId,
                notificationId,
                snoozeTime
            )
            PendingIntent.getBroadcast(
                context,
                notificationId,
                snooze,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

}