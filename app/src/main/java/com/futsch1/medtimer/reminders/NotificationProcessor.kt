package com.futsch1.medtimer.reminders

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.requestStockHandling
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
class NotificationProcessor(val context: Context) {
    private val medicineRepository = MedicineRepository(context.applicationContext as Application?)

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
        ScheduleNextReminderNotificationProcessor(context).scheduleNextReminder()
    }

    fun cancelNotification(notificationId: Int) {
        Log.d(LogTags.REMINDER, "Cancel notification nID $notificationId")
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
    }

    fun removeRemindersFromNotification(reminderEvents: List<ReminderEvent>) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationId = reminderEvents.firstOrNull()?.notificationId
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                val reminderNotificationData = ReminderNotificationData.fromBundle(notification.notification.extras)
                val reminderEventIds = reminderEvents.map { it.reminderEventId }
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
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(context, medicineRepository, newReminderNotificationData)
        if (reminderNotification != null) {
            Notifications(context).showNotification(reminderNotification, reminderNotificationData.notificationId)
        } else {
            cancelNotification(reminderNotificationData.notificationId)
        }
    }

    fun setReminderEventStatus(status: ReminderStatus, reminderEvents: List<ReminderEvent>) {
        for (reminderEvent in reminderEvents) {
            reminderEvent.status = status
            reminderEvent.processedTimestamp = Instant.now().epochSecond
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
            AlarmProcessor(context).cancelPendingReminderNotifications(reminderEvent.reminderEventId)
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
                    requestStockHandling(context, amount, reminder.medicineRelId, reminderEvent.processedTimestamp)
                }
            }
        }
    }
}
