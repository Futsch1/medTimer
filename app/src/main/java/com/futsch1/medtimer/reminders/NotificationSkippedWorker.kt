package com.futsch1.medtimer.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData.Companion.fromData

/**
 * [Worker] implementation that handles the action of a user skipping a medication reminder.
 *
 * This worker processes the notification data passed via [inputData] and updates the
 * corresponding reminder events to the [ReminderEvent.ReminderStatus.SKIPPED] status
 * using the [NotificationProcessor].
 */
class NotificationSkippedWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        NotificationProcessor(applicationContext).processReminderEventsInNotification(fromData(inputData), ReminderEvent.ReminderStatus.SKIPPED)
        return Result.success()
    }
}
