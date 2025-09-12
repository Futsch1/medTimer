package com.futsch1.medtimer.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_ID
import com.futsch1.medtimer.ActivityCodes.EXTRA_NOTIFICATION_TIME_STRING
import com.futsch1.medtimer.ActivityCodes.EXTRA_REMINDER_EVENT_ID
import com.futsch1.medtimer.R
import com.futsch1.medtimer.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent


data class ReminderNotificationData(
    val remindTime: String,
    val medicine: FullMedicine,
    val reminder: Reminder,
    val reminderEvent: ReminderEvent,
    val hasSameTimeReminders: Boolean
)

fun getReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
): ReminderNotificationFactory {
    val defaultPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    return if (defaultPreferences.getBoolean("big_notifications", false)) {
        BigReminderNotificationFactory(
            context, notificationId, reminderNotificationData
        )
    } else {
        SimpleReminderNotificationFactory(
            context, notificationId, reminderNotificationData
        )
    }
}

abstract class ReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    reminderNotificationData: ReminderNotificationData
) : NotificationFactory(context, notificationId, reminderNotificationData.medicine.medicine) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val intents = NotificationIntentBuilder(context, notificationId, reminderNotificationData.reminderEvent, reminderNotificationData.reminder)
    val notificationStrings =
        NotificationStringBuilder(context, reminderNotificationData.medicine, reminderNotificationData.reminder, reminderNotificationData.remindTime)

    val remindTime = reminderNotificationData.remindTime
    val medicine = reminderNotificationData.medicine
    val reminder = reminderNotificationData.reminder
    val reminderEvent = reminderNotificationData.reminderEvent
    val hasSameTimeReminders = reminderNotificationData.hasSameTimeReminders

    init {
        val contentIntent: PendingIntent? = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule).setContentTitle(context.getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setCategory(Notification.CATEGORY_REMINDER).setContentIntent(contentIntent)

        builder.setDeleteIntent(intents.pendingDismiss)

        // Later than Android 14, make notification ongoing so that it cannot be dismissed from the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && defaultSharedPreferences.getBoolean(
                "sticky_on_lockscreen", false
            )
        ) {
            builder.setOngoing(true)
        }
        // If shown as alarm, add a full screen intent
        if (medicine.medicine.showNotificationAsAlarm) {
            addFullScreenIntent()
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun addFullScreenIntent() {
        val intent = Intent(context, ReminderAlarmActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        val bundle = Bundle()
        bundle.putInt(EXTRA_REMINDER_EVENT_ID, reminderEvent.reminderEventId)
        bundle.putInt(EXTRA_NOTIFICATION_ID, notificationId)
        bundle.putString(EXTRA_NOTIFICATION_TIME_STRING, remindTime)
        intent.putExtras(bundle)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        builder.setCategory(Notification.CATEGORY_ALARM)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setFullScreenIntent(pendingIntent, true)
    }

    abstract fun build()

    override fun create(): Notification {
        build()
        return builder.build()
    }


    fun buildActions(
    ) {
        val dismissNotificationAction: String? = defaultSharedPreferences.getString("dismiss_notification_action", "0")

        when (dismissNotificationAction) {
            "0" -> {
                addTakenAction()
                addSnoozeAction()
            }

            "1" -> {
                addTakenAction()
                addSkippedAction()
            }

            else -> {
                addSkippedAction()
                addSnoozeAction()
            }
        }
        if (hasSameTimeReminders) {
            builder.addAction(
                R.drawable.check2_all, context.getString(R.string.all_taken, remindTime), intents.pendingAllTaken
            )
        }
    }

    private fun addSkippedAction() {
        builder.addAction(
            R.drawable.x_circle, context.getString(R.string.skipped), intents.pendingSkipped
        )
    }

    private fun addSnoozeAction() {
        builder.addAction(
            R.drawable.hourglass_split, context.getString(R.string.snooze), intents.pendingSnooze
        )
    }

    private fun addTakenAction() {
        builder.addAction(
            R.drawable.check2_circle, context.getString(R.string.taken), intents.pendingTaken
        )
    }

}