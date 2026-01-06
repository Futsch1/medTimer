package com.futsch1.medtimer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.WorkManagerAccess
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Duration
import java.time.temporal.ChronoUnit

class ReminderWorkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val workManager = WorkManagerAccess.getWorkManager(context)
        when (intent.action) {
            ActivityCodes.DISMISSED_ACTION -> workManager.enqueue(buildActionWorkRequest(intent, SkippedWorker::class.java))
            ActivityCodes.TAKEN_ACTION -> workManager.enqueue(buildActionWorkRequest(intent, TakenWorker::class.java))
            ActivityCodes.SNOOZE_ACTION -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)
                builder.putInt(ActivityCodes.EXTRA_SNOOZE_TIME, intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 15))
                val snoozeWork: WorkRequest =
                    OneTimeWorkRequest.Builder(SnoozeWorker::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(snoozeWork)
            }

            ActivityCodes.REMINDER_ACTION -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)

                val reminderNotificationWorker: WorkRequest =
                    OneTimeWorkRequest.Builder(ReminderNotificationWorker::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(reminderNotificationWorker)
            }
        }
    }

    companion object {
        @JvmStatic
        fun requestScheduleNextNotification(context: Context) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val setAlarmForReminderNotification =
                OneTimeWorkRequest.Builder(ScheduleNextReminderNotificationWorker::class.java)
                    .setInitialDelay(Duration.of(500, ChronoUnit.MILLIS))
                    .build()
            workManager.enqueueUniqueWork("reschedule", ExistingWorkPolicy.REPLACE, setAlarmForReminderNotification)
        }

        @JvmStatic
        @JvmOverloads
        fun requestScheduleNowForTests(context: Context, delay: Long = 0, repeats: Int = 0) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val setAlarmForReminderNotification =
                OneTimeWorkRequest.Builder(ScheduleNextReminderNotificationWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putLong(ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS, delay)
                            .putInt(ActivityCodes.EXTRA_REMAINING_REPEATS, repeats)
                            .build()
                    )
                    .build()
            workManager.enqueue(setAlarmForReminderNotification)
        }

        fun requestRepeat(context: Context, reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Int) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val builder = Data.Builder()
            reminderNotificationData.toBuilder(builder)
            builder.putInt(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, repeatTimeSeconds)
            val repeatWork =
                OneTimeWorkRequest.Builder(RepeatWorker::class.java)
                    .setInputData(builder.build())
                    .build()
            workManager.enqueue(repeatWork)
        }

        @JvmStatic
        fun requestStockHandling(context: Context?, amount: Double, medicineId: Int) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val stockHandlingWorker =
                OneTimeWorkRequest.Builder(StockHandlingWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putDouble(ActivityCodes.EXTRA_AMOUNT, amount)
                            .putInt(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
                            .build()
                    )
                    .build()
            workManager.enqueue(stockHandlingWorker)
        }

        fun requestShowReminderNotification(context: Context, reminderNotificationData: ReminderNotificationData) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val builder = Data.Builder()
            reminderNotificationData.toBuilder(builder)
            val scheduleWork =
                OneTimeWorkRequest.Builder(ShowReminderNotificationWorker::class.java)
                    .setInputData(builder.build())
                    .build()
            workManager.enqueue(scheduleWork)
        }

        fun requestReminderAction(context: Context, processedNotificationData: ProcessedNotificationData, taken: Boolean) {
            val actionIntent: Intent =
                if (taken) getTakenActionIntent(context, processedNotificationData) else getSkippedActionIntent(context, processedNotificationData)
            if (taken) {
                WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, TakenWorker::class.java))
            } else {
                WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, SkippedWorker::class.java))
            }
        }

        private fun <T : ListenableWorker> buildActionWorkRequest(intent: Intent, workerClass: Class<T>): WorkRequest {
            val builder = Data.Builder()
            ProcessedNotificationData.forwardToBuilder(intent.extras!!, builder)
            return OneTimeWorkRequest.Builder(workerClass)
                .setInputData(builder.build())
                .build()
        }
    }
}
