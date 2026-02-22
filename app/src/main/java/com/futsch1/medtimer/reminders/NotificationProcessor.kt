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
class NotificationProcessor(val reminderContext: ReminderContext) {
    private val medicineRepository = reminderContext.medicineRepository

    fun processReminderEventsInNotification(processedNotificationData: ProcessedNotificationData, status: ReminderStatus) {
        Log.d(LogTags.REMINDER, "Process reminder events in notification $processedNotificationData")
        val reminderEventsToUpdate = mutableListOf<ReminderEvent>()
        for (reminderEventId in processedNotificationData.reminderEventIds) {
            val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)

            if (reminderEvent != null) {
                reminderEventsToUpdate.add(reminderEvent)
            } else {
                Log.e(LogTags.REMINDER, "Could not find reminder event reID $reminderEventId in database")
            }
        }

        setReminderEventStatus(status, reminderEventsToUpdate)

        // Reschedule since the trigger condition for a linked reminder might have changed
        ScheduleNextReminderNotificationProcessor(reminderContext).scheduleNextReminder()
    }

    fun cancelNotification(notificationId: Int) {
        Log.d(LogTags.REMINDER, "Cancel notification nID $notificationId")
        reminderContext.notificationManager.cancel(notificationId)
    }

    fun removeRemindersFromNotification(reminderEvents: List<ReminderEvent>) {
        val notificationId = reminderEvents.firstOrNull()?.notificationId
        if (notificationId != null && notificationId != -1) {
            removeRemindersFromNotification(notificationId, reminderEvents.map { it.reminderEventId })
        }
    }

    fun removeRemindersFromNotification(notificationId: Int, reminderEventIds: List<Int>) {
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

    private fun updateNotification(
        reminderNotificationData: ReminderNotificationData,
        reminderEventIds: List<Int>
    ) {
        val newReminderNotificationData = reminderNotificationData.removeReminderEventIds(reminderEventIds)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(reminderContext, newReminderNotificationData)
        if (reminderNotification != null) {
            Notifications(reminderContext).showNotification(reminderNotification, reminderNotificationData.notificationId)
        } else {
            cancelNotification(reminderNotificationData.notificationId)
        }
    }

    fun setReminderEventStatus(status: ReminderStatus, reminderEvents: List<ReminderEvent>) {
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
            AlarmProcessor(reminderContext).cancelPendingReminderNotifications(reminderEvent.reminderEventId)
        }

        medicineRepository.updateReminderEvents(reminderEvents)
        removeRemindersFromNotification(reminderEvents)
    }

    private fun doStockHandling(reminderEvent: ReminderEvent) {
        if (!reminderEvent.stockHandled && reminderEvent.status == ReminderStatus.TAKEN ||
            reminderEvent.stockHandled && reminderEvent.status == ReminderStatus.SKIPPED
        ) {
            val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
            if (reminder != null) {
                var amount: Double? = MedicineHelper.parseAmount(reminderEvent.amount)
                if (amount != null) {
                    if (reminderEvent.status == ReminderStatus.SKIPPED) {
                        amount = -amount
                    }
                    reminderEvent.stockHandled = reminderEvent.status == ReminderStatus.TAKEN
                    StockHandlingProcessor(reminderContext).processStock(
                        amount,
                        reminder.medicineRelId,
                        Instant.ofEpochSecond(reminderEvent.processedTimestamp)
                    )
                }
            }
        }
    }
}
