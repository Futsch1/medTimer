package com.futsch1.medtimer.reminders

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData

class ScheduleWorker(context: Context, workerParams: WorkerParameters) : RescheduleWorker(context, workerParams) {
    override fun doWork(): Result {
        val inputData = getInputData()

        val reminderNotificationData = fromInputData(inputData)
        Log.d(LogTags.REMINDER, "Scheduling reminder: $reminderNotificationData")

        enqueueNotification(reminderNotificationData)

        return Result.success()
    }
}