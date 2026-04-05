package com.futsch1.medtimer.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.R
import com.futsch1.medtimer.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.helpers.MedicineIcons
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.DismissNotificationAction
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification


abstract class ReminderNotificationFactory(
    medicineIcons: MedicineIcons,
    private val context: Context,
    val reminderNotification: ReminderNotification,
    notificationManager: NotificationManager,
    intentsFactory: NotificationIntentBuilder.Factory,
    private val preferencesDataSource: PreferencesDataSource,
    timeFormatter: TimeFormatter
) : NotificationFactory(
    medicineIcons,
    context,
    reminderNotification.reminderNotificationData.notificationId,
    reminderNotification.reminderNotificationParts.map { it.medicine },
    notificationManager
) {

    val intents = intentsFactory.create(reminderNotification)
    val notificationStrings =
        NotificationStringBuilder(
            context,
            preferencesDataSource,
            timeFormatter,
            reminderNotification
        )

    init {
        val contentIntent: PendingIntent = getStartAppIntent()

        builder.setSmallIcon(R.drawable.capsule)
        builder.setContentTitle(context.getString(R.string.notification_title))
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setCategory(android.app.Notification.CATEGORY_REMINDER)
        builder.setContentIntent(contentIntent)

        builder.setDeleteIntent(intents.pendingDismiss)

        builder.setGroup(
            reminderNotification.reminderNotificationData.remindInstant.epochSecond.toString()
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && preferencesDataSource.preferences.value.stickyOnLockscreen
        ) {
            builder.setOngoing(true)
        }
        if (reminderNotification.reminderNotificationParts.any { it.medicine.showNotificationAsAlarm }) {
            addFullScreenIntent()
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun addFullScreenIntent() {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            ReminderAlarmActivity.getIntent(
                context, reminderNotification.reminderNotificationData
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setCategory(android.app.Notification.CATEGORY_ALARM)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setFullScreenIntent(pendingIntent, true)
    }

    abstract fun build()

    override fun create(): android.app.Notification {
        build()
        reminderNotification.reminderNotificationData.toBundle(builder.extras)
        return builder.build()
    }


    fun buildActions(
    ) {
        when (preferencesDataSource.preferences.value.dismissNotificationAction) {
            DismissNotificationAction.SKIP -> {
                addTakenAction()
                addSnoozeAction()
            }

            DismissNotificationAction.SNOOZE -> {
                addTakenAction()
                addSkippedAction()
            }

            else -> {
                addSkippedAction()
                addSnoozeAction()
            }
        }
    }

    private fun addSkippedAction() {
        builder.addAction(
            R.drawable.x_circle, context.getString(R.string.skipped), intents.pendingSkipped
        )
    }

    private fun addSnoozeAction() {
        val action = intents.actionSnoozeRemoteInput
        if (action != null) {
            builder.addAction(action)
        } else {
            builder.addAction(
                R.drawable.hourglass_split, context.getString(R.string.snooze), intents.pendingSnooze
            )
        }
    }

    private fun addTakenAction() {
        val action = intents.actionTaken
        if (action != null) {
            builder.addAction(action)
        } else {
            builder.addAction(
                R.drawable.check2_circle, context.getString(R.string.taken), intents.pendingTaken
            )
        }
    }

}
