package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData
import java.time.Instant

/*
 * Worker that snoozes a reminder and re-raises it once the snooze time has expired.
 */
open class SnoozeWork(context: Context, workerParams: WorkerParameters) : RescheduleWork(context, workerParams) {
    override fun doWork(): Result {
        val inputData = getInputData()
        val snoozeTime = inputData.getInt(ActivityCodes.EXTRA_SNOOZE_TIME, 15)

        val reminderNotificationData = fromInputData(inputData)
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime * 60L)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        // Cancel a potential repeat alarm
        NotificationProcessor.cancelPendingAlarms(context, reminderNotificationData.reminderEventIds[0])

        enqueueNotification(reminderNotificationData)

        NotificationProcessor.cancelNotification(context, reminderNotificationData.notificationId, -1)

        return Result.success()
    }
}
