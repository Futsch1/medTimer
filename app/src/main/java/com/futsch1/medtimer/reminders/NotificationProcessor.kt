package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant
import javax.inject.Inject

/**
 * Processes actions related to medicine reminder notifications, such as marking medications
 * as taken or skipped, updating the database, and managing notification states.
 *
 * This class handles:
 * - Updating [ReminderEvent] statuses and processing stock management.
 * - Modifying or canceling active notifications when reminder events are processed.
 * - Triggering UI for variable dosage amounts.
 * - Scheduling follow-up notifications or rescheduling after status changes.

 */
class NotificationProcessor @Inject constructor(
    val reminderContext: ReminderContext,
    val alarmProcessor: AlarmProcessor,
    val notifications: Notifications,
    val scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor,
    val stockHandlingProcessor: StockHandlingProcessor
) {
    suspend fun processReminderEventsInNotification(processedNotificationData: ProcessedNotificationData, status: ReminderStatus) {
        Log.d(LogTags.REMINDER, "Process reminder events in notification $processedNotificationData")
        val reminderEventsToUpdate = mutableListOf<ReminderEvent>()
        for (reminderEventId in processedNotificationData.reminderEventIds) {
            val reminderEvent = reminderContext.medicineRepository.getReminderEvent(reminderEventId)

            if (reminderEvent != null) {
                reminderEventsToUpdate.add(reminderEvent)
            } else {
                Log.e(LogTags.REMINDER, "Could not find reminder event reID $reminderEventId in database")
            }
        }

        setReminderEventStatus(status, reminderEventsToUpdate)

        // Reschedule since the trigger condition for a linked reminder might have changed
        scheduleNextReminderNotificationProcessor.scheduleNextReminder()
    }

    fun cancelNotification(notificationId: Int) {
        Log.d(LogTags.REMINDER, "Cancel notification nID $notificationId")
        reminderContext.notificationManager.cancel(notificationId)
    }

    suspend fun removeRemindersFromNotification(reminderEvents: List<ReminderEvent>) {
        val notificationId = reminderEvents.firstOrNull()?.notificationId ?: return
        if (notificationId != -1) {
            removeRemindersFromNotification(notificationId, reminderEvents.map { it.reminderEventId })
        }
    }

    suspend fun removeRemindersFromNotification(notificationId: Int, reminderEventIds: List<Int>) {
        Log.d(LogTags.REMINDER, "Remove reminders from notification nID $notificationId")
        for (notification in reminderContext.notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                val reminderNotificationData = ReminderNotificationData.fromBundle(notification.notification.extras)
                reminderNotificationData.notificationId = notificationId
                Log.d(LogTags.REMINDER, "Remove reIDs $reminderEventIds from notification nID $notificationId")
                updateNotification(reminderNotificationData, reminderEventIds)
            }
        }
    }

    private suspend fun updateNotification(
        reminderNotificationData: ReminderNotificationData,
        reminderEventIds: List<Int>
    ) {
        val newReminderNotificationData = reminderNotificationData.removeReminderEventIds(reminderEventIds)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(reminderContext, newReminderNotificationData)
        if (reminderNotification != null) {
            notifications.showNotification(reminderNotification, reminderNotificationData.notificationId)
            rescheduleRepeat(newReminderNotificationData)
        } else {
            cancelNotification(reminderNotificationData.notificationId)
        }
    }

    private suspend fun rescheduleRepeat(reminderNotificationData: ReminderNotificationData) {
        val preferences = reminderContext.preferencesDataSource.preferences.value
        if (!preferences.repeatReminders) {
            return
        }

        val remainingRepeats = reminderContext.medicineRepository
            .getReminderEvent(reminderNotificationData.reminderEventIds[0])
            ?.remainingRepeats ?: return

        if (remainingRepeats > 0) {
            RepeatProcessor(reminderContext).processRepeat(
                reminderNotificationData,
                preferences.repeatDelay
            )
        }
    }

    suspend fun setReminderEventStatus(status: ReminderStatus, reminderEvents: List<ReminderEvent>) {
        for (reminderEvent in reminderEvents) {
            reminderEvent.status = status
            reminderEvent.processedTimestamp = reminderContext.timeAccess.now().epochSecond
            doStockHandling(reminderEvent)
            Log.i(
                LogTags.REMINDER, String.format(
                    "%s reminder reID %d for %s (%s)",
                    status.toString(),
                    reminderEvent.reminderEventId,
                    reminderEvent.medicineName,
                    reminderEvent.amount
                )
            )
            alarmProcessor.cancelPendingReminderNotifications(reminderEvent.reminderEventId)
        }

        reminderContext.medicineRepository.updateReminderEvents(reminderEvents)
        removeRemindersFromNotification(reminderEvents)
    }

    private suspend fun doStockHandling(reminderEvent: ReminderEvent) {
        if (!reminderEvent.stockHandled && reminderEvent.status == ReminderStatus.TAKEN ||
            reminderEvent.stockHandled && reminderEvent.status == ReminderStatus.SKIPPED
        ) {
            val reminder = reminderContext.medicineRepository.getReminder(reminderEvent.reminderId) ?: return
            var amount = MedicineHelper.parseAmount(reminderEvent.amount) ?: return
            if (reminderEvent.status == ReminderStatus.SKIPPED) {
                amount = -amount
            }
            reminderEvent.stockHandled = reminderEvent.status == ReminderStatus.TAKEN
                    stockHandlingProcessor.processStock(
                amount,
                reminder.medicineRelId,
                Instant.ofEpochSecond(reminderEvent.processedTimestamp)
            )
        }
    }
}
