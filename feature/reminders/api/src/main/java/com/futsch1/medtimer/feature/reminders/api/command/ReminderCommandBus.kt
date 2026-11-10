package com.futsch1.medtimer.feature.reminders.api.command

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import java.time.Instant
import kotlin.time.Duration

/**
 * Typed in-process entry point for reminder-feature commands.
 *
 * Callers depend on this interface to trigger reminder work without going through the
 * intent round-trip. The receiver inside the impl module still exists for OS-reentry
 * (AlarmManager, notification action buttons, geofence transitions) and forwards parsed
 * intents to this bus.
 */
interface ReminderCommandBus {
    suspend fun scheduleNextNotification()

    suspend fun showReminderNotification(data: ReminderNotificationData)

    suspend fun showReminders(data: ReminderNotificationData)

    suspend fun snooze(data: ReminderNotificationData, duration: Duration)

    suspend fun processLocationSnooze(data: ReminderNotificationData)

    suspend fun restoreLocationSnoozes()

    /**
     * Marks the reminder events with [status]. [processedTime] records when the dose really
     * happened, for a dose logged after the fact. Without it the events record the current time.
     */
    suspend fun markReminderEvents(
        reminderEventIds: List<Int>,
        status: ReminderEvent.ReminderStatus,
        processedTime: Instant? = null
    )

    /** Re-posts the notification without the given events, or cancels it when none are left. */
    suspend fun removeRemindersFromNotification(notificationId: Int, reminderEventIds: List<Int>)

    suspend fun processStockHandling(amount: Double, medicineId: Int, processedEpochSeconds: Long): Double?

    suspend fun processRefill(medicineId: Int)

    suspend fun processRefill(reminderEventIds: List<Int>)
}
