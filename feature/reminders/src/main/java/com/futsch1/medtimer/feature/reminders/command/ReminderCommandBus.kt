package com.futsch1.medtimer.feature.reminders.command

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import kotlin.time.Duration

/**
 * Typed in-process entry point for reminder-feature commands.
 *
 * Callers depend on this interface to trigger reminder work without going through the
 * [com.futsch1.medtimer.feature.reminders.ReminderProcessorBroadcastReceiver] intent
 * round-trip. The receiver itself still exists for OS-reentry (AlarmManager, notification
 * action buttons, geofence transitions) and forwards parsed intents to this bus.
 */
interface ReminderCommandBus {
    suspend fun scheduleNextNotification()

    suspend fun showReminderNotification(data: ReminderNotificationData)

    suspend fun showReminders(data: ReminderNotificationData)

    suspend fun snooze(data: ReminderNotificationData, duration: Duration)

    suspend fun processLocationSnooze(data: ReminderNotificationData)

    suspend fun markReminderEvents(reminderEventIds: List<Int>, status: ReminderEvent.ReminderStatus)

    suspend fun processStockHandling(amount: Double, medicineId: Int, processedEpochSeconds: Long)

    suspend fun processRefill(medicineId: Int)

    suspend fun processRefill(reminderEventIds: List<Int>)
}
