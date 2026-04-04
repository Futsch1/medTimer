package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.ReminderRepository
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import java.time.Instant
import javax.inject.Inject

/**
 * Processes actions related to medicine reminder notifications, such as marking medications
 * as taken or skipped, updating the database, and managing notification states.
 *
 * This class handles:
 * - Updating [ReminderEventEntity] statuses and processing stock management.
 * - Modifying or canceling active notifications when reminder events are processed.
 * - Triggering UI for variable dosage amounts.
 * - Scheduling follow-up notifications or rescheduling after status changes.

 */
class NotificationProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val notifications: Notifications,
    private val scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor,
    private val stockHandlingProcessor: StockHandlingProcessor,
    private val repeatProcessor: RepeatProcessor,
    private val notificationManager: NotificationManager,
    private val reminderNotificationFactory: ReminderNotificationFactory,
    private val reminderRepository: ReminderRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val timeAccess: TimeAccess
) {
    suspend fun processReminderEventsInNotification(processedNotificationData: ProcessedNotificationData, status: ReminderEvent.ReminderStatus) {
        Log.d(LogTags.REMINDER, "Process reminder events in notification $processedNotificationData")
        val reminderEventsToUpdate = mutableListOf<ReminderEvent>()
        for (reminderEventId in processedNotificationData.reminderEventIds) {
            val reminderEvent = reminderEventRepository.get(reminderEventId)

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
        notificationManager.cancel(notificationId)
    }

    suspend fun removeRemindersFromNotification(reminderEvents: List<ReminderEvent>) {
        val notificationId = reminderEvents.firstOrNull()?.notificationId ?: return
        if (notificationId != -1) {
            removeRemindersFromNotification(notificationId, reminderEvents.map { it.reminderEventId })
        }
    }

    suspend fun removeRemindersFromNotification(notificationId: Int, reminderEventIds: List<Int>) {
        Log.d(LogTags.REMINDER, "Remove reminders from notification nID $notificationId")
        for (notification in notificationManager.activeNotifications) {
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
        val reminderNotification = reminderNotificationFactory.create(newReminderNotificationData)
        if (reminderNotification != null) {
            notifications.showNotification(reminderNotification, reminderNotificationData.notificationId)
            rescheduleRepeat(newReminderNotificationData)
        } else {
            cancelNotification(reminderNotificationData.notificationId)
        }
    }

    private suspend fun rescheduleRepeat(reminderNotificationData: ReminderNotificationData) {
        val preferences = preferencesDataSource.preferences.value
        if (!preferences.repeatReminders) {
            return
        }

        val remainingRepeats = reminderEventRepository
            .get(reminderNotificationData.reminderEventIds[0])
            ?.remainingRepeats ?: return

        if (remainingRepeats > 0) {
            repeatProcessor.processRepeat(
                reminderNotificationData,
                preferences.repeatDelay
            )
        }
    }

    suspend fun setReminderEventStatus(status: ReminderEvent.ReminderStatus, reminderEvents: List<ReminderEvent>) {
        val processedTime = timeAccess.now()

        val reminderEvents = reminderEvents.map { reminderEvent ->
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
            reminderEvent.copy(status = status, processedTimestamp = processedTime, stockHandled = doStockHandling(status, reminderEvent, processedTime))
        }

        reminderEventRepository.updateAll(reminderEvents)
        removeRemindersFromNotification(reminderEvents)
    }

    private suspend fun doStockHandling(status: ReminderEvent.ReminderStatus, reminderEvent: ReminderEvent, processedTime: Instant): Boolean {
        if (reminderEvent.stockHandled && status == ReminderEvent.ReminderStatus.TAKEN ||
            !reminderEvent.stockHandled && status == ReminderEvent.ReminderStatus.SKIPPED
        ) {
            return false
        }
        val reminder = reminderRepository.get(reminderEvent.reminderId) ?: return false
        var amount = MedicineHelper.parseAmount(reminderEvent.amount) ?: return false
        if (status == ReminderEvent.ReminderStatus.SKIPPED) {
            amount = -amount
        }
        stockHandlingProcessor.processStock(
            amount,
            reminder.medicineRelId,
            processedTime
        )
        return status == ReminderEvent.ReminderStatus.TAKEN
    }
}
