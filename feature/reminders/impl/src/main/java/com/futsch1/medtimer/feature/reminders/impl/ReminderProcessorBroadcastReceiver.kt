package com.futsch1.medtimer.feature.reminders.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.futsch1.medtimer.core.common.ActivityCodes
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.common.ProcessorCode
import com.futsch1.medtimer.core.common.di.ApplicationScope
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import com.futsch1.medtimer.feature.reminders.api.notificationData.reminderEventIdsFromBundle
import com.futsch1.medtimer.feature.reminders.api.notificationData.toReminderNotificationData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * OS-reentry adapter that parses incoming intents from AlarmManager, notification action buttons,
 * and geofence transitions, then forwards them to [ReminderCommandBus] for processing.
 */
@AndroidEntryPoint
class ReminderProcessorBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var commandBus: ReminderCommandBus

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = ProcessorCode.fromAction(intent.action!!) ?: return
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                Log.d(LogTags.REMINDER, "Received intent $intentAction")
                dispatch(intentAction, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun dispatch(action: ProcessorCode, intent: Intent) {
        when (action) {
            ProcessorCode.Dismissed -> commandBus.markReminderEvents(
                reminderEventIdsFromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.SKIPPED
            )

            ProcessorCode.Taken -> commandBus.markReminderEvents(
                reminderEventIdsFromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.TAKEN
            )

            ProcessorCode.Acknowledged -> commandBus.markReminderEvents(
                reminderEventIdsFromBundle(intent.extras!!),
                ReminderEvent.ReminderStatus.ACKNOWLEDGED
            )

            ProcessorCode.Snooze -> commandBus.snooze(
                intent.extras!!.toReminderNotificationData(),
                intent.getLongExtra(ActivityCodes.EXTRA_SNOOZE_TIME_SECONDS, 0).toDuration(DurationUnit.SECONDS)
            )

            ProcessorCode.Reminder -> commandBus.showReminders(
                (intent.extras ?: Bundle()).toReminderNotificationData()
            )

            ProcessorCode.ShowReminderNotification -> commandBus.showReminderNotification(
                intent.extras!!.toReminderNotificationData()
            )

            ProcessorCode.Refill -> if (intent.hasExtra(ActivityCodes.EXTRA_MEDICINE_ID)) {
                commandBus.processRefill(intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0))
            } else {
                commandBus.processRefill(reminderEventIdsFromBundle(intent.extras!!))
            }

            ProcessorCode.StockHandling -> commandBus.processStockHandling(
                intent.getDoubleExtra(ActivityCodes.EXTRA_AMOUNT, 0.0),
                intent.getIntExtra(ActivityCodes.EXTRA_MEDICINE_ID, 0),
                intent.getLongExtra(ActivityCodes.EXTRA_REMIND_INSTANT, 0)
            )

            ProcessorCode.Schedule -> commandBus.scheduleNextNotification()

            ProcessorCode.LocationSnooze -> commandBus.processLocationSnooze(
                (intent.extras ?: Bundle()).toReminderNotificationData()
            )

            ProcessorCode.GeofenceEntered -> commandBus.restoreLocationSnoozes()
        }
    }

    companion object {
        const val RECEIVER_PERMISSION = "com.futsch1.medtimer.NOTIFICATION_PROCESSED"

        /**
         * Test-only seam: configures [AlarmProcessor]'s debug delay/repeat globals and fires a
         * Schedule broadcast so instrumented tests exercise the OS-reentry path end-to-end.
         */
        fun requestScheduleNowForTests(context: Context, delay: Long = 0, repeats: Int = 0) {
            AlarmProcessor.delay = delay
            AlarmProcessor.repeats = repeats

            val intent = getRequestScheduleIntent(context)
            context.sendBroadcast(intent, RECEIVER_PERMISSION)
        }
    }
}
