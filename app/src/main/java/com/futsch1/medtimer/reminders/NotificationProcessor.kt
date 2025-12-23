package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestReschedule
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestStockHandling
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant

class NotificationProcessor(val context: Context) {
    private val medicineRepository = MedicineRepository(context.applicationContext as Application?)

    fun processReminderEventsInNotification(processedNotificationData: ProcessedNotificationData, status: ReminderStatus) {
        Log.d(LogTags.REMINDER, "Process reminder events in notification $processedNotificationData")
        val reminderEventsToUpdate = mutableListOf<ReminderEvent>()
        for (reminderEventId in processedNotificationData.reminderEventIds) {
            val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)

            if (reminderEvent != null) {
                if (reminderEvent.askForAmount && status == ReminderStatus.TAKEN) {
                    askForAmount(reminderEvent, reminderEventId)
                } else {
                    reminderEventsToUpdate.add(reminderEvent)
                }
            } else {
                Log.e(LogTags.REMINDER, String.format("Could not find reminder event reID %d in database", reminderEventId))
            }
        }

        setReminderEventStatus(status, reminderEventsToUpdate)

        // Reschedule since the trigger condition for a linked reminder might have changed
        requestReschedule(context)
    }

    private fun askForAmount(reminderEvent: ReminderEvent, reminderEventId: Int) {
        val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
        if (reminder != null) {
            val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
            if (medicine != null) {
                Log.d(LogTags.REMINDER, String.format("Ask for amount for reminder event reID %d", reminderEventId))
                context.startActivity(getVariableAmountActionIntent(context, reminderEventId, reminderEvent.amount, medicine.medicine.name))
                removeRemindersFromNotification(listOf(reminderEvent))
            }
        }
    }

    fun cancelNotification(notificationId: Int) {
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
                Log.d(LogTags.REMINDER, String.format("Remove reIDs %s from notification nID %d", reminderEventIds, notificationId))
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
            Log.d(LogTags.REMINDER, String.format("Notification now empty, cancel nID %d", reminderNotificationData.notificationId))
            cancelNotification(reminderNotificationData.notificationId)
        }
    }

    fun setReminderEventStatus(status: ReminderStatus, reminderEvents: List<ReminderEvent>) {
        for (reminderEvent in reminderEvents) {
            reminderEvent.status = status
            doStockHandling(reminderEvent)
            reminderEvent.processedTimestamp = Instant.now().epochSecond
            Log.i(
                LogTags.REMINDER, String.format(
                    "%s reminder reID %d for %s",
                    if (status == ReminderStatus.TAKEN) "Taken" else "Skipped",
                    reminderEvent.reminderEventId,
                    reminderEvent.medicineName
                )
            )
        }

        medicineRepository.updateReminderEvents(reminderEvents)
        removeRemindersFromNotification(reminderEvents)
    }

    fun cancelPendingAlarms(reminderEventId: Int) {
        val snoozePendingIntent: PendingIntent = PendingIntentBuilder(context).setReminderEventId(reminderEventId).build()
        Log.d(LogTags.REMINDER, String.format("Cancel all pending alarms for reID %d", reminderEventId))
        context.getSystemService(AlarmManager::class.java).cancel(snoozePendingIntent)
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
                    requestStockHandling(context, amount, reminder.medicineRelId)
                }
            }
        }
    }
}
