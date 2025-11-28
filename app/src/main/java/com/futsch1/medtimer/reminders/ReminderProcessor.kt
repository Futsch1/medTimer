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
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.WorkManagerAccess
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Duration
import java.time.temporal.ChronoUnit

class ReminderProcessor : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val workManager = WorkManagerAccess.getWorkManager(context)
        when (intent.action) {
            ActivityCodes.DISMISSED_ACTION -> workManager.enqueue(buildActionWorkRequest(intent, SkippedWorkProcess::class.java))
            ActivityCodes.TAKEN_ACTION -> workManager.enqueue(buildActionWorkRequest(intent, TakenWorkProcess::class.java))
            ActivityCodes.SNOOZE_ACTION -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)
                builder.putInt(ActivityCodes.EXTRA_SNOOZE_TIME, intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 15))
                val snoozeWork: WorkRequest =
                    OneTimeWorkRequest.Builder(SnoozeWork::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(snoozeWork)
            }

            ActivityCodes.REMINDER_ACTION -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)

                val reminderWork: WorkRequest =
                    OneTimeWorkRequest.Builder(ReminderWork::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(reminderWork)
            }
        }
    }

    companion object {
        @JvmStatic
        fun requestReschedule(context: Context) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val rescheduleWork =
                OneTimeWorkRequest.Builder(RescheduleWork::class.java)
                    .setInitialDelay(Duration.of(500, ChronoUnit.MILLIS))
                    .build()
            workManager.enqueueUniqueWork("reschedule", ExistingWorkPolicy.REPLACE, rescheduleWork)
        }

        @JvmStatic
        @JvmOverloads
        fun requestRescheduleNowForTests(context: Context, delay: Long = 0, repeats: Int = 0) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val rescheduleWork =
                OneTimeWorkRequest.Builder(RescheduleWork::class.java)
                    .setInputData(
                        Data.Builder()
                            .putLong(ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS, delay)
                            .putInt(ActivityCodes.EXTRA_REMAINING_REPEATS, repeats)
                            .build()
                    )
                    .build()
            workManager.enqueue(rescheduleWork)
        }

        fun requestRepeat(context: Context, reminderNotificationData: ReminderNotificationData, repeatTimeSeconds: Int) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val builder = Data.Builder()
            reminderNotificationData.toBuilder(builder)
            builder.putInt(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, repeatTimeSeconds)
            val repeatWork =
                OneTimeWorkRequest.Builder(RepeatReminderWork::class.java)
                    .setInputData(builder.build())
                    .build()
            workManager.enqueue(repeatWork)
        }

        @JvmStatic
        fun getReminderAction(context: Context): Intent {
            val reminderIntent = Intent(ActivityCodes.REMINDER_ACTION)
            reminderIntent.setClass(context, ReminderProcessor::class.java)
            return reminderIntent
        }

        fun getSnoozeIntent(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int): Intent {
            val snoozeIntent = Intent(ActivityCodes.SNOOZE_ACTION)
            reminderNotificationData.toIntent(snoozeIntent)
            snoozeIntent.putExtra(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeTime)
            snoozeIntent.setClass(context, ReminderProcessor::class.java)
            return snoozeIntent
        }

        @JvmStatic
        fun requestStockHandling(context: Context?, amount: String?, medicineId: Int) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val stockHandlingWork =
                OneTimeWorkRequest.Builder(StockHandlingWork::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString(ActivityCodes.EXTRA_AMOUNT, amount)
                            .putInt(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
                            .build()
                    )
                    .build()
            workManager.enqueue(stockHandlingWork)
        }

        fun requestActionIntent(context: Context, processedNotificationData: ProcessedNotificationData, taken: Boolean) {
            val actionIntent: Intent =
                if (taken) getTakenActionIntent(context, processedNotificationData) else getSkippedActionIntent(context, processedNotificationData)
            if (taken) {
                WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, TakenWorkProcess::class.java))
            } else {
                WorkManagerAccess.getWorkManager(context).enqueue(buildActionWorkRequest(actionIntent, SkippedWorkProcess::class.java))
            }
        }

        fun getTakenActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
            return buildActionIntent(context, processedNotificationData, ActivityCodes.TAKEN_ACTION)
        }

        fun getSkippedActionIntent(context: Context, processedNotificationData: ProcessedNotificationData): Intent {
            return buildActionIntent(context, processedNotificationData, ActivityCodes.DISMISSED_ACTION)
        }

        private fun <T : ListenableWorker> buildActionWorkRequest(intent: Intent, workerClass: Class<T>): WorkRequest {
            val builder = Data.Builder()
            ProcessedNotificationData.forwardToBuilder(intent.extras!!, builder)
            return OneTimeWorkRequest.Builder(workerClass)
                .setInputData(builder.build())
                .build()
        }

        private fun buildActionIntent(context: Context, processedNotificationData: ProcessedNotificationData, actionName: String?): Intent {
            val actionIntent = Intent(context, ReminderProcessor::class.java)
            processedNotificationData.toIntent(actionIntent)
            actionIntent.setAction(actionName)
            return actionIntent
        }

        @JvmStatic
        fun getVariableAmountActionIntent(context: Context?, reminderEventId: Int, amount: String?): Intent {
            val actionIntent = Intent(context, MainActivity::class.java)
            actionIntent.setAction("VARIABLE_AMOUNT")
            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            actionIntent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID, reminderEventId)
            actionIntent.putExtra(ActivityCodes.EXTRA_AMOUNT, amount)
            return actionIntent
        }

        fun getCustomSnoozeActionIntent(context: Context?, reminderNotificationData: ReminderNotificationData): Intent {
            val actionIntent = Intent(context, MainActivity::class.java)
            actionIntent.setAction("CUSTOM_SNOOZE")
            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reminderNotificationData.toIntent(actionIntent)
            return actionIntent
        }
    }
}
