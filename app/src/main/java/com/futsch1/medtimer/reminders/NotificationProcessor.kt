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
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestReschedule
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestStockHandling
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant

object NotificationProcessor {
    fun processNotification(context: Context, processedNotificationData: ProcessedNotificationData, status: ReminderStatus?) {
        val medicineRepository = MedicineRepository(context as Application?)

        for (reminderEventId in processedNotificationData.reminderEventIds) {
            val reminderEvent = medicineRepository.getReminderEvent(reminderEventId)

            if (reminderEvent != null) {
                if (reminderEvent.askForAmount && status == ReminderStatus.TAKEN) {
                    val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
                    val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
                    Log.d(LogTags.REMINDER, String.format("Ask for amount for reminder event reID %d", reminderEventId))
                    context.startActivity(getVariableAmountActionIntent(context, reminderEventId, reminderEvent.amount, medicine.medicine.name))
                    removeReminderFromNotification(context, reminderEvent.notificationId, reminderEventId)
                } else {
                    processReminderEvent(context, status, reminderEvent, medicineRepository)
                }
            } else {
                Log.e(LogTags.REMINDER, String.format("Could not find reminder event reID %d in database", reminderEventId))
            }
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
    }

    fun removeReminderFromNotification(context: Context, notificationId: Int, reminderEventId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                val reminderNotificationData = ReminderNotificationData.fromBundle(notification.notification.extras)
                Log.d(LogTags.REMINDER, String.format("Remove reID %d from notification nID %d", reminderEventId, notificationId))
                updateNotification(reminderNotificationData, reminderEventId, context)
            }
        }
    }

    private fun updateNotification(
        reminderNotificationData: ReminderNotificationData,
        reminderEventId: Int,
        context: Context
    ) {
        val newReminderNotificationData = reminderNotificationData.removeReminderEventIds(listOf(reminderEventId))
        val medicineRepository = MedicineRepository(context.applicationContext as Application?)
        val reminderNotification = ReminderNotification.fromReminderNotificationData(context, medicineRepository, newReminderNotificationData)
        if (reminderNotification != null) {
            if (reminderNotificationData.reminderEventIds.isEmpty()) {
                cancelNotification(context, reminderNotificationData.notificationId)
            } else {
                Notifications(context).showNotification(reminderNotification, reminderNotificationData.notificationId)
            }
        }
    }

    fun processReminderEvent(context: Context, status: ReminderStatus?, reminderEvent: ReminderEvent, medicineRepository: MedicineRepository) {
        removeReminderFromNotification(context, reminderEvent.notificationId, reminderEvent.reminderEventId)

        reminderEvent.status = status
        doStockHandling(context, reminderEvent, medicineRepository)
        reminderEvent.processedTimestamp = Instant.now().epochSecond

        medicineRepository.updateReminderEvent(reminderEvent)
        Log.i(
            LogTags.REMINDER, String.format(
                "%s reminder reID %d for %s",
                if (status == ReminderStatus.TAKEN) "Taken" else "Dismissed",
                reminderEvent.reminderEventId,
                reminderEvent.medicineName
            )
        )

        // Reschedule since the trigger condition for a linked reminder might have changed
        requestReschedule(context)
    }

    fun cancelPendingAlarms(context: Context, reminderEventId: Int) {
        val snoozePendingIntent: PendingIntent = PendingIntentBuilder(context).setReminderEventId(reminderEventId).build()
        Log.d(LogTags.REMINDER, String.format("Cancel all pending alarms for reID %d", reminderEventId))
        context.getSystemService(AlarmManager::class.java).cancel(snoozePendingIntent)
    }

    private fun doStockHandling(context: Context?, reminderEvent: ReminderEvent, medicineRepository: MedicineRepository) {
        if (!reminderEvent.stockHandled && reminderEvent.status == ReminderStatus.TAKEN) {
            reminderEvent.stockHandled = true
            val reminder = medicineRepository.getReminder(reminderEvent.reminderId)
            if (reminder != null) {
                requestStockHandling(context, reminder.amount, reminder.medicineRelId)
            }
        }
    }
}
