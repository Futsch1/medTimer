package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData
import java.time.Instant

/**
 * [Worker] implementation that handles the snooze functionality for reminders.
 *
 * This worker calculates a new reminder time based on the provided snooze duration,
 * cancels any existing notifications or pending alarms for the reminder, and
 * schedules a new alarm for the future.
 */
open class SnoozeWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = AlarmProcessor(context)

    override fun doWork(): Result {
        val snoozeTime = inputData.getInt(ActivityCodes.EXTRA_SNOOZE_TIME, 15)

        val reminderNotificationData = fromInputData(inputData)
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime * 60L)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        // Cancel a potential repeat alarm
        AlarmProcessor(context).cancelPendingReminderNotifications(reminderNotificationData)

        alarmSetter.setAlarmForReminderNotification(reminderNotificationData)

        NotificationProcessor(context).cancelNotification(reminderNotificationData.notificationId)

        return Result.success()
    }
}
