package com.futsch1.medtimer.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver.Companion.RECEIVER_PERMISSION
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.widgets.WidgetUpdateReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class AlarmProcessor @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val timeAccess: TimeAccess,
    preferencesDataSource: PreferencesDataSource
) {
    private val exactReminders: Boolean = preferencesDataSource.preferences.value.exactReminders

    // Slot 0 — owns the next-scheduled-reminder alarm.
    // AlarmManager.set with same PendingIntent atomically replaces, so no explicit cancel.
    suspend fun setNextReminderAlarm(
        scheduledReminderNotificationData: ReminderNotificationData,
        reminderNotificationProcessor: ReminderNotificationProcessor
    ): Boolean {
        scheduledReminderNotificationData.remindInstant = adjustTimestamp(timeAccess, scheduledReminderNotificationData.remindInstant)

        if (!scheduledReminderNotificationData.remindInstant.isAfter(timeAccess.now())) {
            Log.i(
                LogTags.REMINDER,
                String.format("Show reminder notification now: %s", scheduledReminderNotificationData)
            )
            reminderNotificationProcessor.processReminders(scheduledReminderNotificationData)
            return false
        }

        scheduleAlarm(scheduledReminderNotificationData.remindInstant, scheduledReminderNotificationData.getPendingIntent(context))

        Log.i(
            LogTags.REMINDER,
            String.format("Set alarm for reminder notification: %s", scheduledReminderNotificationData)
        )

        updateNextReminderWidget()

        return true
    }

    // Per-event slots — snooze/repeat/show/location-snooze. Leaves slot 0 untouched.
    fun setSecondaryAlarm(reminderNotificationData: ReminderNotificationData): Boolean {
        reminderNotificationData.remindInstant = adjustTimestamp(timeAccess, reminderNotificationData.remindInstant)

        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            if (reminderEventId != 0) {
                cancelPendingReminderNotifications(reminderEventId)
            }
        }

        if (!reminderNotificationData.remindInstant.isAfter(timeAccess.now())) {
            Log.i(
                LogTags.REMINDER,
                String.format("Show reminder notification now: %s", reminderNotificationData)
            )
            val reminderIntent = getReminderAction(context)
            reminderNotificationData.toIntent(reminderIntent)
            context.sendBroadcast(reminderIntent, RECEIVER_PERMISSION)
            return false
        }

        scheduleAlarm(reminderNotificationData.remindInstant, reminderNotificationData.getPendingIntent(context))

        Log.i(
            LogTags.REMINDER,
            String.format("Set secondary alarm for reminder notification: %s", reminderNotificationData)
        )

        updateNextReminderWidget()

        return true
    }

    private fun scheduleAlarm(instant: Instant, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant.toEpochMilli(), pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, instant.toEpochMilli(), pendingIntent)
        }
    }

    fun cancelNextReminder() {
        val intent = getReminderAction(context)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    fun cancelPendingReminderNotifications(reminderEventId: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, reminderEventId,
                getReminderAction(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        Log.d(LogTags.REMINDER, "Cancel pending reminder notification alarms for reID $reminderEventId")
    }

    fun cancelPendingReminderNotifications(reminderNotificationData: ReminderNotificationData) {
        for (id in reminderNotificationData.reminderEventIds) {
            cancelPendingReminderNotifications(id)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactReminders && alarmManager.canScheduleExactAlarms()
        } else {
            exactReminders
        }
    }

    private fun updateNextReminderWidget() {
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = "com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE"
        }

        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
    }

    companion object {
        fun adjustTimestamp(timeAccess: TimeAccess, instant: Instant): Instant {
            return if (delay >= 0) {
                Log.d(LogTags.REMINDER, "Debug schedule reminder in $delay milliseconds")
                val instantDebug = timeAccess.now().plusMillis(delay)
                repeats -= 1
                if (repeats < 0) {
                    delay = -1
                }
                instantDebug
            } else {
                instant
            }
        }

        var delay: Long = -1
        var repeats: Int = -1
    }
}
