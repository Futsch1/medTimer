package com.futsch1.medtimer.reminders

import android.Manifest.permission
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper
import java.time.ZoneId
import java.util.stream.Collectors

class ReminderWork(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private lateinit var medicineRepository: MedicineRepository

    override fun doWork(): Result {
        val inputData = getInputData()

        medicineRepository = MedicineRepository(applicationContext as Application)

        val r = processReminders(inputData)

        medicineRepository.flushDatabase()

        // Reminder shown, now schedule next reminder
        ReminderProcessor.requestReschedule(context)

        return r
    }

    private fun processReminders(inputData: Data): Result {
        var r = Result.failure()

        val reminderNotificationData = ReminderNotificationData.fromInputData(inputData, medicineRepository)
        reminderNotificationData.createReminderEvents(numberOfRepeats)

        if (reminderNotificationData.valid) {
            performActionsOfReminders(reminderNotificationData)
            r = Result.success()
        }

        return r
    }

    private val numberOfRepeats: Int
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString(PreferencesNames.NUMBER_OF_REPETITIONS, "3")!!.toInt()
        }

    private fun performActionsOfReminders(reminderNotificationData: ReminderNotificationData) {
        for (notificationReminderEvent in reminderNotificationData.notificationReminderEvents) {
            if (notificationReminderEvent.reminder.automaticallyTaken) {
                NotificationProcessor.processReminderEvent(
                    context,
                    ReminderEvent.ReminderStatus.TAKEN,
                    notificationReminderEvent.reminderEvent,
                    medicineRepository
                )
                Log.i(
                    LogTags.REMINDER,
                    String.format(
                        "Mark reminder reID %d as automatically taken for %s",
                        notificationReminderEvent.reminderEvent.reminderEventId,
                        notificationReminderEvent.reminderEvent.medicineName
                    )
                )
            }
        }

        notificationAction(reminderNotificationData.filterAutomaticallyTaken())
    }

    private fun notificationAction(reminderNotificationData: ReminderNotificationData) {
        for (notificationReminderEvent in reminderNotificationData.notificationReminderEvents) {
            NotificationProcessor.cancelNotification(context, notificationReminderEvent.reminderEvent.notificationId)

            Log.i(
                LogTags.REMINDER,
                String.format(
                    "Show reminder event reID %d for %s",
                    notificationReminderEvent.reminderEvent.reminderEventId,
                    notificationReminderEvent.reminderEvent.medicineName
                )
            )
        }

        // Schedule remaining repeats for all reminders
        val remainingRepeats = reminderNotificationData.notificationReminderEvents[0].reminderEvent.remainingRepeats
        if (remainingRepeats != 0 && this.isRepeatReminders) {
            ReminderProcessor.requestRepeat(context, reminderNotificationData, repeatTimeSeconds)
        }

        // Show notifications for all reminders
        showNotification(reminderNotificationData)
    }

    private fun showNotification(reminderNotificationData: ReminderNotificationData) {
        if (canShowNotifications()) {
            val notifications = Notifications(context)
            val notificationId =
                notifications.showNotification(
                    reminderNotificationData
                )

            for (notificationReminderEvent in reminderNotificationData.notificationReminderEvents) {
                notificationReminderEvent.reminderEvent.notificationId = notificationId
                medicineRepository.updateReminderEvent(notificationReminderEvent.reminderEvent)
            }
        }
    }

    private val isRepeatReminders: Boolean
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getBoolean(PreferencesNames.REPEAT_REMINDERS, false)
        }

    private fun canShowNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private val repeatTimeSeconds: Int
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString(PreferencesNames.REPEAT_DELAY, "10")!!.toInt() * 60
        }

    companion object {
        fun buildReminderEvent(remindedTimeStamp: Long, medicine: FullMedicine, reminder: Reminder, medicineRepository: MedicineRepository): ReminderEvent {
            val reminderEvent = ReminderEvent()
            reminderEvent.reminderId = reminder.reminderId
            reminderEvent.remindedTimestamp = remindedTimeStamp
            reminderEvent.amount = reminder.amount
            reminderEvent.medicineName = medicine.medicine.name + CyclesHelper.getCycleCountString(reminder)
            reminderEvent.color = medicine.medicine.color
            reminderEvent.useColor = medicine.medicine.useColor
            reminderEvent.status = ReminderEvent.ReminderStatus.RAISED
            reminderEvent.iconId = medicine.medicine.iconId
            reminderEvent.askForAmount = reminder.variableAmount
            reminderEvent.tags = medicine.tags.stream().map { t: Tag? -> t!!.name }.collect((Collectors.toList()))
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

        private fun getLastReminderEventTimeInMinutes(medicineRepository: MedicineRepository, reminderEvent: ReminderEvent, isWindowedInterval: Boolean): Int {
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
