package com.futsch1.medtimer.reminders.notificationFactory

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper.getMedicineNameWithStockTextForNotification
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
    remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
) : NotificationFactory(context, notificationId, medicine.medicine) {
    val defaultSharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    init {
        val contentIntent: PendingIntent? = getStartAppIntent()

        val notificationMessage: String = getNotificationString(remindTime, reminder, medicine)
        builder.setSmallIcon(R.drawable.capsule)
            .setContentTitle(context.getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions(builder, notificationId, reminderEvent.reminderEventId, reminder)
    }

    private fun buildActions(
        builder: NotificationCompat.Builder,
        notificationId: Int,
        reminderEventId: Int,
        reminder: Reminder
    ) {
        val dismissNotificationAction: String? =
            defaultSharedPreferences.getString("dismiss_notification_action", "0")
        val snoozeTime = defaultSharedPreferences.getString("snooze_duration", "15")!!.toInt()
        val stickyOnLockscreen: Boolean =
            defaultSharedPreferences.getBoolean("sticky_on_lockscreen", false)

        val pendingSnooze: PendingIntent? = getSnoozePendingIntent(
            context,
            reminder.reminderId,
            reminderEventId,
            notificationId,
            snoozeTime
        )

        val notifyDismissed = ReminderProcessor.getDismissedActionIntent(context, reminderEventId)
        val pendingDismissed = PendingIntent.getBroadcast(
            context,
            notificationId,
            notifyDismissed,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pendingTaken: PendingIntent? =
            getTakenPendingIntent(notificationId, reminderEventId, reminder)

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
            builder.setDeleteIntent(pendingDismissed)
        } else if (dismissNotificationAction == "1") {
            builder.addAction(
                R.drawable.check2_circle,
                context.getString(R.string.taken),
                pendingTaken
            )
            builder.addAction(
                R.drawable.x_circle,
                context.getString(R.string.skipped),
                pendingDismissed
            )
            builder.setDeleteIntent(pendingSnooze)
        } else {
            builder.addAction(
                R.drawable.x_circle,
                context.getString(R.string.skipped),
                pendingDismissed
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


    private fun getNotificationString(
        remindTime: String?,
        reminder: Reminder,
        medicine: FullMedicine
    ): String {
        var instructions = reminder.instructions
        if (instructions == null) {
            instructions = ""
        }
        if (instructions.isNotEmpty()) {
            instructions = " $instructions"
        }
        val amountStringId: Int =
            if (reminder.amount.isBlank()) R.string.notification_content_blank else R.string.notification_content
        val medicineNameString =
            getMedicineNameWithStockTextForNotification(context, medicine.medicine)
        val notificationString = context.getString(
            amountStringId,
            remindTime,
            reminder.amount,
            medicineNameString,
            instructions
        )
        val tagNames =
            medicine.tags.stream().map<String?> { t: Tag? -> t!!.name }.collect(Collectors.toList())
        if (tagNames.isEmpty()) {
            return notificationString
        }
        return notificationString + "\n(" + java.lang.String.join(", ", tagNames) + ")"
    }

    private fun getSnoozePendingIntent(
        context: Context,
        reminderId: Int,
        reminderEventId: Int,
        notificationId: Int,
        snoozeTime: Int
    ): PendingIntent? {
        return if (snoozeTime == -1) {
            val snooze = ReminderProcessor.getCustomSnoozeActionIntent(
                context,
                reminderId,
                reminderEventId,
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
                reminderId,
                reminderEventId,
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

    private fun getTakenPendingIntent(
        notificationId: Int,
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

    override fun create(): Notification {
        return builder.build()
    }
}