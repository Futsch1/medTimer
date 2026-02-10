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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
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
    val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    override fun onReceive(context: Context, intent: Intent) {
        val workManager = WorkManagerAccess.getWorkManager(context)
        val intentAction = WorkerActionCode.fromAction(intent.action!!)
        when (intentAction) {
            WorkerActionCode.Dismissed -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.SKIPPED
            )

            WorkerActionCode.Taken -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.TAKEN
            )

            WorkerActionCode.Acknowledged -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )

            WorkerActionCode.Snooze -> {
                processSnoozeAsync(context, intent)
            }

            WorkerActionCode.Reminder -> {
                processReminderNotificationAsync(context, ReminderNotificationData.fromBundle(intent.extras!!))
            }

            WorkerActionCode.ShowReminderNotification -> {
                val builder = Data.Builder()
                ReminderNotificationData.forwardToBuilder(intent.extras!!, builder)

                val reminderNotificationWorker: WorkRequest =
                    OneTimeWorkRequest.Builder(ShowReminderNotificationWorker::class.java)
                        .setInputData(builder.build())
                        .build()
                workManager.enqueue(reminderNotificationWorker)
            }

            WorkerActionCode.Refill -> workManager.enqueue(buildActionWorkRequest(intent, RefillWorker::class.java))
            WorkerActionCode.StockHandling -> processStockHandlingAsync(context, intent)
            WorkerActionCode.Repeat -> processRepeatAsync(context, intent)
            null -> Unit
        }
    }

    private fun processSnoozeAsync(context: Context, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            SnoozeProcessor(context).processSnooze(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 0)
            )
            pendingIntent.finish()
        }
    }

    private fun processRepeatAsync(context: Context, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            RepeatProcessor(context).processRepeat(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0)
            )
            pendingIntent.finish()
        }
    }

    private fun processStockHandlingAsync(context: Context, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            val amount = intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0)
            val medicineId = intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0)
            val processedInstant = Instant.ofEpochSecond(intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0))

            StockHandlingProcessor(context).processStock(amount, medicineId, processedInstant)

            pendingIntent.finish()
        }
    }

    private fun processReminderNotificationAsync(
        context: Context,
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            ReminderNotificationProcessor(reminderNotificationData, context).processReminders()
            pendingIntent.finish()
        }
    }

    private fun processNotificationAsync(
        context: Context,
        processedNotificationData: ProcessedNotificationData,
        status: ReminderEvent.ReminderStatus
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            NotificationProcessor(context).processReminderEventsInNotification(processedNotificationData, status)

            pendingIntent.finish()
        }
    }

    companion object {
        const val RECEIVER_PERMISSION = "com.futsch1.medtimer.NOTIFICATION_PROCESSED"

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
            AlarmProcessor.delay = delay
            AlarmProcessor.repeats = repeats

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
            context.sendBroadcast(getRepeatIntent(context, reminderNotificationData, repeatTimeSeconds), RECEIVER_PERMISSION)
        }

        fun requestStockHandling(context: Context?, amount: Double, medicineId: Int, processedEpochSeconds: Long) {
            val intent = getStockHandlingIntent(context!!, amount, medicineId, processedEpochSeconds)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestShowReminderNotification(context: Context, reminderNotificationData: ReminderNotificationData) {
            val intent = getShowReminderNotificationIntent(context, reminderNotificationData)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestSnooze(context: Context, reminderNotificationData: ReminderNotificationData, snoozeTime: Int) {
            val intent = getSnoozeIntent(context, reminderNotificationData, snoozeTime)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestReminderAction(context: Context, reminder: Reminder?, reminderEvent: ReminderEvent, taken: Boolean) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))

            if (taken) {
                if (reminder?.variableAmount == true) {
                    context.startActivity(getVariableAmountActivityIntent(context, ReminderNotificationData.fromReminderEvent(reminderEvent)))
                } else {
                    context.sendBroadcast(getTakenActionIntent(context, processedNotificationData), RECEIVER_PERMISSION)
                }
            } else {
                context.sendBroadcast(getSkippedActionIntent(context, processedNotificationData), RECEIVER_PERMISSION)
            }
        }

        fun requestStockReminderAcknowledged(context: Context, reminderEvent: ReminderEvent) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))
            context.sendBroadcast(getAcknowledgedActionIntent(context, processedNotificationData), RECEIVER_PERMISSION)
        }

        fun requestRefill(context: Context, medicineId: Int) {
            val intent = getRefillActionIntent(context, ProcessedNotificationData(listOf()))
            intent.putExtra(ActivityCodes.EXTRA_MEDICINE_ID, medicineId)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
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
