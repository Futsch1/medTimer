package com.futsch1.medtimer.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.reminders.NotificationProcessor.processNotification
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData.Companion.fromData

open class ProcessNotificationWorker(context: Context, workerParams: WorkerParameters, private val status: ReminderStatus?) : Worker(context, workerParams) {
    override fun doWork(): Result {
        processNotification(applicationContext, fromData(inputData), status)
        return Result.success()
    }
}
