package com.futsch1.medtimer.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData.Companion.fromData

class TakenWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        NotificationProcessor(applicationContext).processReminderEventsInNotification(fromData(inputData), ReminderEvent.ReminderStatus.TAKEN)
        return Result.success()
    }
}