package com.futsch1.medtimer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ProcessorCode
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * [BroadcastReceiver] that acts as the central entry point for reminder-related events and background tasks.
 *
 * It also provides static utility methods in its [companion object] to programmatically schedule
 * various reminder tasks such as rescheduling notifications, handling stock updates, and
 * repeating alerts.
 */
class ReminderProcessorBroadcastReceiver : BroadcastReceiver() {
    val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = ProcessorCode.fromAction(intent.action!!)
        when (intentAction) {
            ProcessorCode.Dismissed -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.SKIPPED
            )

            ProcessorCode.Taken -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.TAKEN
            )

            ProcessorCode.Acknowledged -> processNotificationAsync(
                context,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )

            ProcessorCode.Snooze -> {
                processSnoozeAsync(context, intent)
            }

            ProcessorCode.Reminder -> {
                processReminderNotificationAsync(context, ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.ShowReminderNotification -> {
                processShowReminderNotificationAsync(context, ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.Refill -> processRefillAsync(context, intent)
            ProcessorCode.StockHandling -> processStockHandlingAsync(context, intent)
            ProcessorCode.Repeat -> processRepeatAsync(context, intent)
            ProcessorCode.Schedule -> processRescheduleAsync(context)
            null -> Unit
        }
    }

    private fun processRescheduleAsync(context: Context) {
        val pendingIntent = goAsync()

        scope.launch {
            ScheduleNextReminderNotificationProcessor(context).scheduleNextReminder()

            pendingIntent.finish()
        }
    }

    private fun processShowReminderNotificationAsync(
        context: Context,
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            ShowReminderNotificationProcessor(context).showReminder(reminderNotificationData)

            pendingIntent.finish()
        }
    }

    private fun processRefillAsync(context: Context, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
                RefillProcessor(context).processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
            } else {
                RefillProcessor(context).processRefill(ProcessedNotificationData.fromBundle(intent.extras!!))
            }
            pendingIntent.finish()
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
            val intent = getRequestScheduleIntent(context)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        @JvmStatic
        @JvmOverloads
        fun requestScheduleNowForTests(context: Context, delay: Long = 0, repeats: Int = 0) {
            AlarmProcessor.delay = delay
            AlarmProcessor.repeats = repeats

            requestScheduleNextNotification(context)
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
            val intent = getRefillIntent(context, medicineId)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }
    }
}
