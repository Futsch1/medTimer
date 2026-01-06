package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData
import java.time.Instant

/*
 * Worker that snoozes a reminder and re-raises it once the snooze time has expired.
 */
open class SnoozeWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = SetAlarmForReminderNotification(context)

    override fun doWork(): Result {
        val snoozeTime = inputData.getInt(ActivityCodes.EXTRA_SNOOZE_TIME, 15)

        val reminderNotificationData = fromInputData(inputData)
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime * 60L)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        // Cancel a potential repeat alarm
        val notificationProcessor = NotificationProcessor(context)
        notificationProcessor.cancelPendingAlarms(reminderNotificationData.reminderEventIds[0])

        alarmSetter.setAlarmForReminderNotification(reminderNotificationData, inputData)

        notificationProcessor.cancelNotification(reminderNotificationData.notificationId)

        return Result.success()
    }
}
