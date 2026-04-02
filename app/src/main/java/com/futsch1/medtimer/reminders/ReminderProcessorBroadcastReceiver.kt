package com.futsch1.medtimer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ProcessorCode
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.di.ApplicationScope
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

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

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = ProcessorCode.fromAction(intent.action!!) ?: return
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                when (intentAction) {
                    ProcessorCode.Dismissed -> notificationProcessor.processReminderEventsInNotification(
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEventEntity.ReminderStatus.SKIPPED
                    )

                    ProcessorCode.Taken -> notificationProcessor.processReminderEventsInNotification(
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEventEntity.ReminderStatus.TAKEN
                    )

                    ProcessorCode.Acknowledged -> notificationProcessor.processReminderEventsInNotification(
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEventEntity.ReminderStatus.ACKNOWLEDGED
                    )

                    ProcessorCode.Snooze -> processSnooze(intent)
                    ProcessorCode.Reminder -> reminderNotificationProcessor.processReminders(ReminderNotificationData.fromBundle(intent.extras!!))
                    ProcessorCode.ShowReminderNotification -> showReminderNotificationProcessor.showReminder(
                        ReminderNotificationData.fromBundle(intent.extras!!)
                    )

                    ProcessorCode.Refill -> processRefill(intent)
                    ProcessorCode.StockHandling -> processStockHandling(intent)
                    ProcessorCode.Schedule -> scheduleNextReminderNotificationProcessor.scheduleNextReminder()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processRefill(intent: Intent) {
        if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
            refillProcessor.processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
        } else {
            refillProcessor.processRefill(ProcessedNotificationData.fromBundle(intent.extras!!))
        }
    }

    private fun processSnooze(intent: Intent) {
        snoozeProcessor.processSnooze(
            ReminderNotificationData.fromBundle(intent.extras!!),
            intent.getLongExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 0)
        )
    }

    private suspend fun processStockHandling(intent: Intent) {
        val amount = intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0)
        val medicineId = intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0)
        val processedInstant = Instant.ofEpochSecond(intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0))
        stockHandlingProcessor.processStock(amount, medicineId, processedInstant)
    }

    companion object {
        const val RECEIVER_PERMISSION = "com.futsch1.medtimer.NOTIFICATION_PROCESSED"

        fun requestScheduleNextNotification(context: Context) {
            val intent = getRequestScheduleIntent(context)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

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

        fun requestSnooze(context: Context, reminderNotificationData: ReminderNotificationData, snoozeDuration: Duration) {
            val intent = getSnoozeIntent(context, reminderNotificationData, snoozeDuration)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }

        fun requestReminderAction(context: Context, reminder: ReminderEntity?, reminderEvent: ReminderEventEntity, taken: Boolean) {
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

        fun requestStockReminderAcknowledged(context: Context, reminderEvent: ReminderEventEntity) {
            val processedNotificationData = ProcessedNotificationData(listOf(reminderEvent.reminderEventId))
            context.sendBroadcast(getAcknowledgedActionIntent(context, processedNotificationData), RECEIVER_PERMISSION)
        }

        fun requestRefill(context: Context, medicineId: Int) {
            val intent = getRefillIntent(context, medicineId)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }
    }
}
