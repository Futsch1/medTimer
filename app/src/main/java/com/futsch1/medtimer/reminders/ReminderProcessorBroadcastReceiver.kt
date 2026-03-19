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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

/**
 * [BroadcastReceiver] that acts as the central entry point for reminder-related events and background tasks.
 *
 * It also provides static utility methods in its [companion object] to programmatically schedule
 * various reminder tasks such as rescheduling notifications, handling stock updates, and
 * repeating alerts.
 */
@AndroidEntryPoint
class ReminderProcessorBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationProcessor: NotificationProcessor

    @Inject
    lateinit var snoozeProcessor: SnoozeProcessor

    @Inject
    lateinit var refillProcessor: RefillProcessor

    @Inject
    lateinit var repeatProcessor: RepeatProcessor

    @Inject
    lateinit var reminderNotificationProcessor: ReminderNotificationProcessor

    @Inject
    lateinit var scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor

    @Inject
    lateinit var showReminderNotificationProcessor: ShowReminderNotificationProcessor

    @Inject
    lateinit var stockHandlingProcessor: StockHandlingProcessor


    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = ProcessorCode.fromAction(intent.action!!)
        when (intentAction) {
            ProcessorCode.Dismissed -> processNotificationAsync(
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.SKIPPED
            )

            ProcessorCode.Taken -> processNotificationAsync(
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.TAKEN
            )

            ProcessorCode.Acknowledged -> processNotificationAsync(
                ProcessedNotificationData.fromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )

            ProcessorCode.Snooze -> {
                processSnoozeAsync(intent)
            }

            ProcessorCode.Reminder -> {
                processReminderNotificationAsync(ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.ShowReminderNotification -> {
                processShowReminderNotificationAsync(ReminderNotificationData.fromBundle(intent.extras!!))
            }

            ProcessorCode.Refill -> processRefillAsync(intent)
            ProcessorCode.StockHandling -> processStockHandlingAsync(intent)
            ProcessorCode.Repeat -> processRepeatAsync(intent)
            ProcessorCode.Schedule -> processRescheduleAsync()
            null -> Unit
        }
    }

    private fun processRescheduleAsync() {
        val pendingIntent = goAsync()

        scope.launch {
            scheduleNextReminderNotificationProcessor.scheduleNextReminder()

            pendingIntent.finish()
        }
    }

    private fun processShowReminderNotificationAsync(
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            showReminderNotificationProcessor.showReminder(reminderNotificationData)

            pendingIntent.finish()
        }
    }

    private fun processRefillAsync(intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
                refillProcessor.processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
            } else {
                refillProcessor.processRefill(ProcessedNotificationData.fromBundle(intent.extras!!))
            }
            pendingIntent.finish()
        }
    }

    private fun processSnoozeAsync(intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            snoozeProcessor.processSnooze(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 0)
            )
            pendingIntent.finish()
        }
    }

    private fun processRepeatAsync(intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            repeatProcessor.processRepeat(
                ReminderNotificationData.fromBundle(intent.extras!!),
                intent.getIntExtra(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0).toDuration(DurationUnit.SECONDS)
            )
            pendingIntent.finish()
        }
    }

    private fun processStockHandlingAsync(intent: Intent) {
        val pendingIntent = goAsync()

        scope.launch {
            val amount = intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0)
            val medicineId = intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0)
            val processedInstant = Instant.ofEpochSecond(intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0))

            stockHandlingProcessor.processStock(amount, medicineId, processedInstant)

            pendingIntent.finish()
        }
    }

    private fun processReminderNotificationAsync(
        reminderNotificationData: ReminderNotificationData
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            reminderNotificationProcessor.processReminders(reminderNotificationData)
            pendingIntent.finish()
        }
    }

    private fun processNotificationAsync(
        processedNotificationData: ProcessedNotificationData,
        status: ReminderEvent.ReminderStatus
    ) {
        val pendingIntent = goAsync()

        scope.launch {
            notificationProcessor.processReminderEventsInNotification(processedNotificationData, status)

            pendingIntent.finish()
        }
    }

    companion object {
        const val RECEIVER_PERMISSION = "com.futsch1.medtimer.NOTIFICATION_PROCESSED"

        fun requestScheduleNextNotification(reminderContext: ReminderContext) {
            val intent = getRequestScheduleIntent(reminderContext)
            reminderContext.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestScheduleNowForTests(reminderContext: ReminderContext, delay: Long = 0, repeats: Int = 0) {
            AlarmProcessor.delay = delay
            AlarmProcessor.repeats = repeats

            requestScheduleNextNotification(reminderContext)
        }

        fun requestStockHandling(reminderContext: ReminderContext?, amount: Double, medicineId: Int, processedEpochSeconds: Long) {
            val intent = getStockHandlingIntent(reminderContext!!, amount, medicineId, processedEpochSeconds)
            reminderContext.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestShowReminderNotification(reminderContext: ReminderContext, reminderNotificationData: ReminderNotificationData) {
            val intent = getShowReminderNotificationIntent(reminderContext, reminderNotificationData)
            reminderContext.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestSnooze(context: Context, reminderNotificationData: ReminderNotificationData, snoozeDuration: Duration) {
            val intent = getSnoozeIntent(ReminderContext(context), reminderNotificationData, snoozeDuration)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestReminderAction(reminderContext: ReminderContext, reminder: Reminder?, reminderEvent: ReminderEvent, taken: Boolean) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))

            if (taken) {
                if (reminder?.variableAmount == true) {
                    reminderContext.startActivity(getVariableAmountActivityIntent(reminderContext, ReminderNotificationData.fromReminderEvent(reminderEvent)))
                } else {
                    reminderContext.sendBroadcast(getTakenActionIntent(reminderContext, processedNotificationData), RECEIVER_PERMISSION)
                }
            } else {
                reminderContext.sendBroadcast(getSkippedActionIntent(reminderContext, processedNotificationData), RECEIVER_PERMISSION)
            }
        }

        fun requestStockReminderAcknowledged(reminderContext: ReminderContext, reminderEvent: ReminderEvent) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))
            reminderContext.sendBroadcast(getAcknowledgedActionIntent(reminderContext, processedNotificationData), RECEIVER_PERMISSION)
        }

        fun requestRefill(reminderContext: ReminderContext, medicineId: Int) {
            val intent = getRefillIntent(reminderContext, medicineId)
            reminderContext.sendBroadcast(intent, RECEIVER_PERMISSION)
        }
    }
}
