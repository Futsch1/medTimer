package com.futsch1.medtimer.feature.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReminderNotificationProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val notifications: Notifications,
    val notificationProcessor: NotificationProcessor,
    val repeatProcessor: RepeatProcessor,
    private val reminderNotificationFactory: ReminderNotificationFactory,
    private val reminderEventRepository: ReminderEventRepository,
    private val preferencesDataSource: PreferencesDataSource
) {
    suspend fun processReminders(reminderNotificationData: ReminderNotificationData): Boolean {
        // Create reminder events and filter those that are already processed
        val reminderNotification =
            reminderNotificationFactory.create(reminderNotificationData)?.filterAlreadyProcessed() ?: return false

        val nonTakenReminderNotification = handleAutomaticallyTaken(reminderNotification)
        if (nonTakenReminderNotification.reminderNotificationParts.isEmpty()) {
            Log.d(LogTags.REMINDER, "No reminders left to process in $nonTakenReminderNotification")
        } else {
            Log.d(LogTags.REMINDER, "Processing reminder notification $nonTakenReminderNotification")
            notificationAction(nonTakenReminderNotification)
        }

        return true
    }

    private suspend fun handleAutomaticallyTaken(reminderNotification: ReminderNotification): ReminderNotification {
        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (reminderNotificationPart.reminder.automaticallyTaken) {
                notificationProcessor.setReminderEventStatus(
                    ReminderEvent.ReminderStatus.TAKEN,
                    listOf(reminderNotificationPart.reminderEvent)
                )
                Log.i(
                    LogTags.REMINDER,
                    String.format(
                        "Mark reminder reID %d as automatically taken for %s",
                        reminderNotificationPart.reminderEvent.reminderEventId,
                        reminderNotificationPart.reminderEvent.medicineName
                    )
                )
            }
        }
        return reminderNotification.filterAutomaticallyTaken()
    }

    private suspend fun notificationAction(reminderNotification: ReminderNotification) {
        if (reminderNotification.reminderNotificationData.notificationId != -1) {
            notificationProcessor.cancelNotification(reminderNotification.reminderNotificationData.notificationId)
        }

        // Show notifications for all reminders
        showNotification(reminderNotification)

        // Schedule remaining repeats for all reminders
        val remainingRepeats = reminderNotification.reminderNotificationParts[0].reminderEvent.remainingRepeats
        if (remainingRepeats != 0 && preferencesDataSource.preferences.value.repeatReminders) {
            repeatProcessor.processRepeat(
                reminderNotification.reminderNotificationData,
                preferencesDataSource.preferences.value.repeatDelay
            )
        }
    }

    private suspend fun showNotification(reminderNotification: ReminderNotification) {
        if (canShowNotifications()) {
            val notificationId =
                notifications.showNotification(
                    reminderNotification
                )

            for (notificationReminderEvent in reminderNotification.reminderNotificationParts) {
                reminderEventRepository.update(notificationReminderEvent.reminderEvent.copy(notificationId = notificationId))
            }
        }
    }

    private fun canShowNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
