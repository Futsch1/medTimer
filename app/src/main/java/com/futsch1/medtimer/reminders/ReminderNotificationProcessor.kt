package com.futsch1.medtimer.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.database.toModelReminderEventType
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.model.reminderevent.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationFactory
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
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

        val processedEvents = nonTakenReminderNotification.reminderNotificationParts.map { it.reminderEvent }
        scheduleNextReminderNotificationProcessor.scheduleNextReminder(processedEvents)

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

    companion object {
        suspend fun buildReminderEvent(
            remindedTimeStamp: Long,
            medicine: FullMedicineEntity,
            reminder: ReminderEntity,
            reminderEventRepository: ReminderEventRepository,
            timeFormatter: TimeFormatter
        ): ReminderEvent {
            val remindedInstant = Instant.ofEpochSecond(remindedTimeStamp)
            val amount = when (reminder.reminderType) {
                ReminderEntity.ReminderType.OUT_OF_STOCK -> {
                    MedicineHelper.formatAmount(medicine.medicine.amount, medicine.medicine.unit)
                }

                ReminderEntity.ReminderType.EXPIRATION_DATE -> {
                    timeFormatter.daysSinceEpochToDateString(medicine.medicine.expirationDate)
                }

                else -> {
                    reminder.amount
                }
            }

            val lastIntervalReminderTimeInMinutes = if (reminder.isInterval) {
                getLastReminderEventTimeInMinutes(
                    reminderEventRepository,
                    reminder.reminderId,
                    remindedInstant,
                    reminder.reminderType == ReminderEntity.ReminderType.WINDOWED_INTERVAL
                )
            } else {
                0
            }

            return ReminderEvent(
                reminderEventId = 0,
                reminderId = reminder.reminderId,
                reminder = null,
                medicineName = medicine.medicine.name + CyclesHelper.getCycleCountString(reminder),
                amount = amount,
                color = medicine.medicine.color,
                useColor = medicine.medicine.useColor,
                status = ReminderEvent.ReminderStatus.RAISED,
                remindedTimestamp = remindedInstant,
                processedTimestamp = Instant.EPOCH,
                notificationId = 0,
                iconId = medicine.medicine.iconId,
                remainingRepeats = 0,
                notes = "",
                reminderType = reminder.reminderType.toModelReminderEventType(),
                stockHandled = false,
                askForAmount = reminder.variableAmount,
                tags = medicine.tags.map { it.name },
                lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes
            )
        }

        private suspend fun getLastReminderEventTimeInMinutes(
            reminderEventRepository: ReminderEventRepository,
            reminderId: Int,
            remindedTimestamp: Instant,
            isWindowedInterval: Boolean
        ): Int {
            val lastReminderEvent = reminderEventRepository.getLast(reminderId)
            return if (lastReminderEvent != null && lastReminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
                if (isWindowedInterval && TimeHelper.secondsSinceEpochToLocalDate(
                        lastReminderEvent.remindedTimestamp.epochSecond,
                        ZoneId.systemDefault()
                    ) != TimeHelper.secondsSinceEpochToLocalDate(remindedTimestamp.epochSecond, ZoneId.systemDefault())
                ) {
                    0
                } else {
                    (lastReminderEvent.processedTimestamp.epochSecond / 60).toInt()
                }
            } else {
                0
            }
        }
    }
}
