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
        val reminderContext = ReminderContext(context)
        val intentAction = ProcessorCode.fromAction(intent.action!!)
        when (intentAction) {
            ProcessorCode.Dismissed -> processNotificationAsync(
                reminderContext,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.SKIPPED
            )

            ProcessorCode.Taken -> processNotificationAsync(
                reminderContext,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.TAKEN
            )

            ProcessorCode.Acknowledged -> processNotificationAsync(
                reminderContext,
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )

            ProcessorCode.Snooze -> {
                processSnoozeAsync(reminderContext, intent)
            }

            ProcessorCode.Reminder -> {
                processReminderNotificationAsync(reminderContext, ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.ShowReminderNotification -> {
                processShowReminderNotificationAsync(reminderContext, ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.Refill -> processRefillAsync(reminderContext, intent)
            ProcessorCode.StockHandling -> processStockHandlingAsync(reminderContext, intent)
            ProcessorCode.Repeat -> processRepeatAsync(reminderContext, intent)
            ProcessorCode.Schedule -> processRescheduleAsync(reminderContext)
            null -> Unit
        }
    }

    private fun processRescheduleAsync(reminderContext: ReminderContext) {
        val pendingIntent = goAsync()

        scope.launch {
            ScheduleNextReminderNotificationProcessor(reminderContext).scheduleNextReminder()

            pendingIntent.finish()
        }
    }

    private fun processShowReminderNotificationAsync(
        reminderContext: ReminderContext,
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            ShowReminderNotificationProcessor(reminderContext).showReminder(reminderNotificationData)

            pendingIntent.finish()
        }
    }

    private fun processRefillAsync(reminderContext: ReminderContext, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
                RefillProcessor(reminderContext).processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
            } else {
                RefillProcessor(reminderContext).processRefill(ProcessedNotificationData.fromBundle(intent.extras!!))
            }
            pendingIntent.finish()
        }
    }

    private fun processSnoozeAsync(reminderContext: ReminderContext, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            SnoozeProcessor(reminderContext).processSnooze(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 0)
            )
            pendingIntent.finish()
        }
    }

    private fun processRepeatAsync(reminderContext: ReminderContext, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            RepeatProcessor(reminderContext).processRepeat(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0)
            )
            pendingIntent.finish()
        }
    }

    private fun processStockHandlingAsync(reminderContext: ReminderContext, intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            val amount = intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0)
            val medicineId = intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0)
            val processedInstant = Instant.ofEpochSecond(intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0))

            StockHandlingProcessor(reminderContext).processStock(amount, medicineId, processedInstant)

            pendingIntent.finish()
        }
    }

    private fun processReminderNotificationAsync(
        reminderContext: ReminderContext,
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            ReminderNotificationProcessor(reminderContext).processReminders(reminderNotificationData)
            pendingIntent.finish()
        }
    }

    private fun processNotificationAsync(
        reminderContext: ReminderContext,
        processedNotificationData: ProcessedNotificationData,
        status: ReminderEvent.ReminderStatus
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            NotificationProcessor(reminderContext).processReminderEventsInNotification(processedNotificationData, status)

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
