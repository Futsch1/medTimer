package com.futsch1.medtimer.reminders

 import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ProcessorCode
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
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
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val reminderContext = ReminderContext(context)
        val intentAction = ProcessorCode.fromAction(intent.action!!) ?: return
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                when (intentAction) {
                    ProcessorCode.Dismissed -> processNotification(
                        reminderContext,
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEvent.ReminderStatus.SKIPPED
                    )

                    ProcessorCode.Taken -> processNotification(
                        reminderContext,
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEvent.ReminderStatus.TAKEN
                    )

                    ProcessorCode.Acknowledged -> processNotification(
                        reminderContext,
                        ProcessedNotificationData.fromBundle(intent.extras!!),
                        ReminderEvent.ReminderStatus.ACKNOWLEDGED
                    )

                    ProcessorCode.Snooze -> processSnooze(reminderContext, intent)
                    ProcessorCode.Reminder -> processReminderNotification(reminderContext, ReminderNotificationData.fromBundle(intent.extras!!))
                    ProcessorCode.ShowReminderNotification -> processShowReminderNotification(
                        reminderContext,
                        ReminderNotificationData.fromBundle(intent.extras!!)
                    )

                    ProcessorCode.Refill -> processRefill(reminderContext, intent)
                    ProcessorCode.StockHandling -> processStockHandling(reminderContext, intent)
                    ProcessorCode.Repeat -> processRepeat(reminderContext, intent)
                    ProcessorCode.Schedule -> processReschedule(reminderContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processReschedule(reminderContext: ReminderContext) {
        ScheduleNextReminderNotificationProcessor(reminderContext).scheduleNextReminder()
    }

    private suspend fun processShowReminderNotification(
        reminderContext: ReminderContext,
        reminderNotificationData: ReminderNotificationData
    ) {
        ShowReminderNotificationProcessor(reminderContext).showReminder(reminderNotificationData)
    }

    private suspend fun processRefill(reminderContext: ReminderContext, intent: Intent) {
        if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
            RefillProcessor(reminderContext).processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
        } else {
            RefillProcessor(reminderContext).processRefill(ProcessedNotificationData.fromBundle(intent.extras!!))
        }
    }

    private suspend fun processSnooze(reminderContext: ReminderContext, intent: Intent) {
        SnoozeProcessor(reminderContext).processSnooze(
            ReminderNotificationData.fromBundle(intent.extras!!),
            intent.getIntExtra(ActivityCodes.EXTRA_SNOOZE_TIME, 0)
        )
    }

    private suspend fun processRepeat(reminderContext: ReminderContext, intent: Intent) {
        RepeatProcessor(reminderContext).processRepeat(
            ReminderNotificationData.fromBundle(intent.extras!!),
            intent.getIntExtra(ActivityCodes.EXTRA_REPEAT_TIME_SECONDS, 0)
        )
    }

    private suspend fun processStockHandling(reminderContext: ReminderContext, intent: Intent) {
        val amount = intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0)
        val medicineId = intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0)
        val processedInstant = Instant.ofEpochSecond(intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0))
        StockHandlingProcessor(reminderContext).processStock(amount, medicineId, processedInstant)
    }

    private suspend fun processReminderNotification(
        reminderContext: ReminderContext,
        reminderNotificationData: ReminderNotificationData
    ) {
        ReminderNotificationProcessor(reminderContext).processReminders(reminderNotificationData)
    }

    private suspend fun processNotification(
        reminderContext: ReminderContext,
        processedNotificationData: ProcessedNotificationData,
        status: ReminderEvent.ReminderStatus
    ) {
        NotificationProcessor(reminderContext).processReminderEventsInNotification(processedNotificationData, status)
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

        fun requestSnooze(context: Context, reminderNotificationData: ReminderNotificationData, snoozeDuration: Duration) {
            val intent = getSnoozeIntent(context, reminderNotificationData, snoozeDuration)
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
