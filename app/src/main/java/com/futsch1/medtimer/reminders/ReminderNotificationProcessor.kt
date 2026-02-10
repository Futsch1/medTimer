package com.futsch1.medtimer.reminders

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.MedicineHelper
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper
import java.time.ZoneId
import java.util.stream.Collectors

class ReminderNotificationProcessor(
    val reminderNotificationData: ReminderNotificationData,
    val context: Context
) {
    val medicineRepository = MedicineRepository(context.applicationContext as Application)

    fun processReminders(): Boolean {
        var r = false

        // Create reminder events and filter those that are already processed
        var reminderNotification =
            ReminderNotification.fromReminderNotificationData(context, medicineRepository, reminderNotificationData)?.filterAlreadyProcessed()

        if (reminderNotification != null) {
            reminderNotification = handleAutomaticallyTaken(reminderNotification)
            if (reminderNotification.reminderNotificationParts.isEmpty()) {
                Log.d(LogTags.REMINDER, "No reminders left to process in $reminderNotification")
            } else {
                Log.d(LogTags.REMINDER, "Processing reminder notification $reminderNotification")
                notificationAction(reminderNotification)
            }
            r = true
        }

        ScheduleNextReminderNotificationProcessor(context).scheduleNextReminder()

        return r
    }

    private fun handleAutomaticallyTaken(reminderNotification: ReminderNotification): ReminderNotification {
        for (reminderNotificationPart in reminderNotification.reminderNotificationParts) {
            if (reminderNotificationPart.reminder.automaticallyTaken) {
                NotificationProcessor(context).setReminderEventStatus(
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

    private fun notificationAction(reminderNotification: ReminderNotification) {
        if (reminderNotification.reminderNotificationData.notificationId != -1) {
            NotificationProcessor(context).cancelNotification(reminderNotification.reminderNotificationData.notificationId)
        }

        // Show notifications for all reminders
        showNotification(reminderNotification)

        // Schedule remaining repeats for all reminders
        val remainingRepeats = reminderNotification.reminderNotificationParts[0].reminderEvent.remainingRepeats
        if (remainingRepeats != 0 && this.isRepeatReminders) {
            ReminderProcessorBroadcastReceiver.requestRepeat(context, reminderNotification.reminderNotificationData, repeatTimeSeconds)
        }
    }

    private fun showNotification(reminderNotification: ReminderNotification) {
        if (canShowNotifications()) {
            val notifications = Notifications(context)
            val notificationId =
                notifications.showNotification(
                    reminderNotification
                )

            for (notificationReminderEvent in reminderNotification.reminderNotificationParts) {
                notificationReminderEvent.reminderEvent.notificationId = notificationId
                medicineRepository.updateReminderEvent(notificationReminderEvent.reminderEvent)
            }
        }
    }

    private val isRepeatReminders: Boolean
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPref.getBoolean(PreferencesNames.REPEAT_REMINDERS, false)
        }

    private fun canShowNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private val repeatTimeSeconds: Int
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPref.getString(PreferencesNames.REPEAT_DELAY, "10")!!.toInt() * 60
        }

    companion object {
        fun buildReminderEvent(remindedTimeStamp: Long, medicine: FullMedicine, reminder: Reminder, medicineRepository: MedicineRepository): ReminderEvent {
            val reminderEvent = ReminderEvent()
            reminderEvent.reminderId = reminder.reminderId
            reminderEvent.remindedTimestamp = remindedTimeStamp
            reminderEvent.medicineName = medicine.medicine.name + CyclesHelper.getCycleCountString(reminder)
            reminderEvent.color = medicine.medicine.color
            reminderEvent.useColor = medicine.medicine.useColor
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED
            reminderEvent.iconId = medicine.medicine.iconId
            reminderEvent.askForAmount = reminder.variableAmount
            reminderEvent.tags = medicine.tags.stream().map { t: Tag? -> t!!.name }.collect((Collectors.toList()))
            reminderEvent.reminderType = reminder.reminderType

            when (reminder.reminderType) {
                Reminder.ReminderType.OUT_OF_STOCK -> {
                    reminderEvent.amount = MedicineHelper.formatAmount(medicine.medicine.amount, medicine.medicine.unit)
                }

                Reminder.ReminderType.EXPIRATION_DATE -> {
                    reminderEvent.amount = TimeHelper.daysSinceEpochToDateString(medicineRepository.application, medicine.medicine.expirationDate)
                }

                else -> {
                    reminderEvent.amount = reminder.amount
                }
            }
            if (reminder.isInterval) {
                reminderEvent.lastIntervalReminderTimeInMinutes = getLastReminderEventTimeInMinutes(
                    medicineRepository,
                    reminderEvent,
                    reminder.reminderType == Reminder.ReminderType.WINDOWED_INTERVAL
                )
            } else {
                reminderEvent.lastIntervalReminderTimeInMinutes = 0
            }

            return reminderEvent
        }

        private fun getLastReminderEventTimeInMinutes(
            medicineRepository: MedicineRepository,
            reminderEvent: ReminderEvent,
            isWindowedInterval: Boolean
        ): Int {
            val lastReminderEvent = medicineRepository.getLastReminderEvent(reminderEvent.reminderId)
            return if (lastReminderEvent != null && lastReminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
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