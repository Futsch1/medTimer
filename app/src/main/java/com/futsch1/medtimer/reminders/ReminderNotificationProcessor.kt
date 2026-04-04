package com.futsch1.medtimer.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.TagEntity
import com.futsch1.medtimer.database.toEntity
import com.futsch1.medtimer.database.toModel
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.util.stream.Collectors
import javax.inject.Inject

class ReminderNotificationProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val notifications: Notifications,
    val notificationProcessor: NotificationProcessor,
    val repeatProcessor: RepeatProcessor,
    val scheduleNextReminderNotificationProcessor: ScheduleNextReminderNotificationProcessor,
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

        val processedEvents = nonTakenReminderNotification.reminderNotificationParts.map { it.reminderEvent.toModel() }
        scheduleNextReminderNotificationProcessor.scheduleNextReminder(processedEvents)

        return true
    }

    private suspend fun handleAutomaticallyTaken(reminderNotification: ReminderNotification): ReminderNotification {
        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (reminderNotificationPart.reminder.automaticallyTaken) {
                notificationProcessor.setReminderEventStatus(
                    ReminderEventEntity.ReminderStatus.TAKEN,
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
                notificationReminderEvent.reminderEvent.notificationId = notificationId
                reminderEventRepository.update(notificationReminderEvent.reminderEvent.toModel())
            }
        }
    }

    private fun canShowNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        suspend fun buildReminderEvent(
            remindedTimeStamp: Long,
            medicine: FullMedicineEntity,
            reminder: ReminderEntity,
            reminderEventRepository: ReminderEventRepository,
            timeFormatter: TimeFormatter
        ): ReminderEventEntity {
            val reminderEvent = ReminderEventEntity()
            reminderEvent.reminderId = reminder.reminderId
            reminderEvent.remindedTimestamp = remindedTimeStamp
            reminderEvent.medicineName = medicine.medicine.name + CyclesHelper.getCycleCountString(reminder)
            reminderEvent.color = medicine.medicine.color
            reminderEvent.useColor = medicine.medicine.useColor
            reminderEvent.status = ReminderEventEntity.ReminderStatus.RAISED
            reminderEvent.iconId = medicine.medicine.iconId
            reminderEvent.askForAmount = reminder.variableAmount
            reminderEvent.tags = medicine.tags.stream().map { t: TagEntity? -> t!!.name }.collect((Collectors.toList()))
            reminderEvent.reminderType = reminder.reminderType

            when (reminder.reminderType) {
                ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                    reminderEvent.amount = MedicineHelper.formatAmount(medicine.medicine.amount, medicine.medicine.unit)
                }

                ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                    reminderEvent.amount =
                        timeFormatter.daysSinceEpochToDateString(medicine.medicine.expirationDate)
                }

                else -> {
                    reminderEvent.amount = reminder.amount
                }
            }
            if (reminder.isInterval) {
                reminderEvent.lastIntervalReminderTimeInMinutes = getLastReminderEventTimeInMinutes(
                    reminderEventRepository,
                    reminderEvent,
                    reminder.reminderType == ReminderEntity.ReminderType.WINDOWED_INTERVAL
                )
            } else {
                reminderEvent.lastIntervalReminderTimeInMinutes = 0
            }

            return reminderEvent
        }

        private suspend fun getLastReminderEventTimeInMinutes(
            reminderEventRepository: ReminderEventRepository,
            reminderEvent: ReminderEventEntity,
            isWindowedInterval: Boolean
        ): Int {
            val lastReminderEvent = reminderEventRepository.getLast(reminderEvent.reminderId)?.toEntity()
            return if (lastReminderEvent != null && lastReminderEvent.status == ReminderEventEntity.ReminderStatus.TAKEN) {
                if (isWindowedInterval && TimeHelper.secondsSinceEpochToLocalDate(
                        lastReminderEvent.remindedTimestamp,
                        ZoneId.systemDefault()
                    ) !== TimeHelper.secondsSinceEpochToLocalDate(reminderEvent.remindedTimestamp, ZoneId.systemDefault())
                ) {
                    0
                } else {
                    (lastReminderEvent.processedTimestamp / 60).toInt()
                }
            } else {
                0
            }
        }
    }
}