package com.futsch1.medtimer.feature.reminders.notificationFactory

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.DismissNotificationAction
import com.futsch1.medtimer.core.ui.MedicineIcons
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.feature.reminders.notificationData.effectiveHighestImportance
import com.futsch1.medtimer.feature.reminders.notificationData.effectiveShowAsAlarm
import com.futsch1.medtimer.feature.reminders.notificationData.writeTo


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
    notificationManager,
    reminderNotification.reminderNotificationParts.effectiveHighestImportance()
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
        if (reminderNotification.reminderNotificationParts.any { it.effectiveShowAsAlarm() }) {
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
        reminderNotification.reminderNotificationData.writeTo(builder.extras)
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

        addLocationSnoozeAction()
    }

    private fun addSkippedAction() {
        intents.pendingSkipped?.let {
            builder.addAction(
                R.drawable.x_circle, context.getString(R.string.skipped), it
            )
        }
    }

    private fun addSnoozeAction() {
        builder.addAction(
            R.drawable.hourglass_split, context.getString(R.string.snooze), intents.pendingSnooze
        )
    }

    private fun addLocationSnoozeAction() {
        if (intents.pendingLocationSnooze != null) {
            builder.addAction(
                R.drawable.geo_alt_fill,
                context.getString(R.string.snooze_until_home),
                intents.pendingLocationSnooze
            )
        }
    }

    private fun addTakenAction() {
        builder.addAction(
            R.drawable.check2_circle, context.getString(R.string.taken), intents.pendingTaken
        )
    }
}
