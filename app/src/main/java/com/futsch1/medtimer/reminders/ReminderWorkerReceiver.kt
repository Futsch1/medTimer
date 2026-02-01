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
import com.futsch1.medtimer.WorkerActionCode
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * [BroadcastReceiver] that acts as the central entry point for reminder-related events and background tasks.
 *
 * This class handles incoming intents from notifications (like Dismiss, Taken, Snooze, or Reminder actions)
 * and delegates them to the appropriate [androidx.work.ListenableWorker] via [androidx.work.WorkManager].
 *
 * It also provides static utility methods in its [companion object] to programmatically schedule
 * various reminder tasks such as rescheduling notifications, handling stock updates, and
 * repeating alerts.
 */
class ReminderWorkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workManager = WorkManagerAccess.getWorkManager(context)
        val intentAction = WorkerActionCode.fromAction(intent.action!!)
        when (intentAction) {
            WorkerActionCode.Dismissed -> workManager.enqueue(buildActionWorkRequest(intent, SkippedWorker::class.java))
            WorkerActionCode.Taken -> workManager.enqueue(buildActionWorkRequest(intent, TakenWorker::class.java))
            WorkerActionCode.Acknowledged -> workManager.enqueue(buildActionWorkRequest(intent, AcknowledgedWorker::class.java))
            WorkerActionCode.Snooze -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)
                builder.putInt(ActivityCodes.EXTRA_SNOOZE_TIME, intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 15))
                val snoozeWork: WorkRequest =
                    OneTimeWorkRequest.Builder(SnoozeWorker::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(snoozeWork)
            }

            WorkerActionCode.Reminder -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)

                val reminderNotificationWorker: WorkRequest =
                    OneTimeWorkRequest.Builder(ReminderNotificationWorker::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(reminderNotificationWorker)
            }

            WorkerActionCode.Refill -> workManager.enqueue(buildActionWorkRequest(intent, RefillWorker::class.java))
            null -> Unit
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
            workManager.enqueueUniqueWork("reschedule", ExistingWorkPolicy.KEEP, setAlarmForReminderNotification)
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

        fun requestStockHandling(context: Context?, amount: Double, medicineId: Int, processedInstant: Long) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val stockHandlingWorker =
                OneTimeWorkRequest.Builder(StockHandlingWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putDouble(ActivityCodes.EXTRA_AMOUNT, amount)
                            .putLong(ActivityCodes.EXTRA_REMIND_INSTANT, processedInstant)
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

        fun requestSnooze(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int) {
            val workManager = WorkManagerAccess.getWorkManager(context)
            val builder = Data.Builder()
            reminderNotificationData.toBuilder(builder)
            builder.putInt(ActivityCodes.EXTRA_SNOOZE_TIME, snoozeTime)
            val snoozeWork = OneTimeWorkRequest.Builder(SnoozeWorker::class.java)
                .setInputData(builder.build())
                .build()
            workManager.enqueue(snoozeWork)
        }

        fun requestReminderAction(context: Context, reminder: Reminder?, reminderEvent: ReminderEvent, taken: Boolean) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))

            if (taken) {
                if (reminder?.variableAmount == true) {
                    context.startActivity(getVariableAmountActivityIntent(context, ReminderNotificationData.fromReminderEvent(reminderEvent)))
                } else {
                    WorkManagerAccess.getWorkManager(context)
                        .enqueue(buildActionWorkRequest(getTakenActionIntent(context, processedNotificationData), TakenWorker::class.java))
                }
            } else {
                WorkManagerAccess.getWorkManager(context)
                    .enqueue(buildActionWorkRequest(getSkippedActionIntent(context, processedNotificationData), SkippedWorker::class.java))
            }
        }

        fun requestStockReminderAcknowledged(context: Context, reminderEvent: ReminderEvent) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))

            WorkManagerAccess.getWorkManager(context)
                .enqueue(buildActionWorkRequest(getAcknowledgedActionIntent(context, processedNotificationData), AcknowledgedWorker::class.java))
        }

        fun requestRefill(context: Context, medicineId: Int) {
            val refillWorker =
                OneTimeWorkRequest.Builder(RefillWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putInt(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
                            .putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, intArrayOf())
                            .build()
                    )
                    .build()
            WorkManagerAccess.getWorkManager(context).enqueue(refillWorker)
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
