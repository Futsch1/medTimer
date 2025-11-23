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
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.Tag
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.scheduling.CyclesHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.stream.Collectors

class ReminderWork(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private lateinit var medicineRepository: MedicineRepository
    private var reminderIds: IntArray? = null
    private var reminderEventIds: IntArray? = null

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

        val reminderDate = LocalDate.ofEpochDay(inputData.getLong(ActivityCodes.EXTRA_REMINDER_DATE, LocalDate.now().toEpochDay()))
        val reminderTime = LocalTime.ofSecondOfDay(inputData.getInt(ActivityCodes.EXTRA_REMINDER_TIME, LocalTime.now().toSecondOfDay()).toLong())
        val reminderDateTime = LocalDateTime.of(reminderDate, reminderTime)
        val reminderTimestamp = reminderDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(reminderDateTime))
        val triplets = getTriplets(inputData, reminderTimestamp)

        if (triplets.isNotEmpty()) {
            performActionsOfReminders(triplets, reminderDateTime)
            r = Result.success()
        }

        return r
    }

    private fun getTriplets(inputData: Data, reminderTimestamp: Long): List<NotificationTriplet> {
        reminderIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)
        reminderEventIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)
        val triplets = ArrayList<NotificationTriplet>()
        if (reminderIds != null && reminderEventIds != null) {

            for (i in reminderIds!!.indices) {
                val reminderId = reminderIds!![i]
                val reminderEventId = reminderEventIds!![i]
                val reminder = medicineRepository.getReminder(reminderId)
                if (reminder == null) {
                    Log.e(LogTags.REMINDER, String.format("Could not find reminder rID %d in database", reminderId))
                    return emptyList()
                }

                val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
                var reminderEvent = getEvent(reminderEventId, reminderTimestamp, reminder)
                if (reminderEvent == null) {
                    reminderEvent = buildAndInsertReminderEvent(reminderTimestamp, medicine!!, reminder)
                }
                triplets.add(NotificationTriplet(reminder, reminderEvent, medicine))
            }
        }
        return triplets
    }

    private fun getEvent(reminderEventId: Int, remindedTimeStamp: Long, reminder: Reminder): ReminderEvent? {
        return if (reminderEventId != 0) {
            medicineRepository.getReminderEvent(reminderEventId)
        } else {
            // We might have created the reminder event already
            medicineRepository.getReminderEvent(reminder.reminderId, remindedTimeStamp)
        }
    }

    private fun buildAndInsertReminderEvent(remindedTimeStamp: Long, medicine: FullMedicine, reminder: Reminder): ReminderEvent {
        val reminderEvent: ReminderEvent = buildReminderEvent(remindedTimeStamp, medicine, reminder, medicineRepository)
        reminderEvent.remainingRepeats = this.numberOfRepeats
        reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()
        return reminderEvent
    }

    private val numberOfRepeats: Int
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            return sharedPref.getString(PreferencesNames.NUMBER_OF_REPETITIONS, "3")!!.toInt()
        }

    private fun performActionsOfReminders(triplets: List<NotificationTriplet>, reminderDateTime: LocalDateTime) {
        for (triplet in triplets) {
            if (triplet.reminder.automaticallyTaken) {
                NotificationAction.processReminderEvent(context, ReminderEvent.ReminderStatus.TAKEN, triplet.reminderEvent, medicineRepository)
                Log.i(
                    LogTags.REMINDER,
                    String.format(
                        "Mark reminder reID %d as automatically taken for %s",
                        triplet.reminderEvent.reminderEventId,
                        triplet.reminderEvent.medicineName
                    )
                )
            }
        }

        notificationAction(triplets.stream().filter { !it.reminder.automaticallyTaken }.toList(), reminderDateTime)
    }

    private fun notificationAction(triplets: List<NotificationTriplet>, reminderDateTime: LocalDateTime) {
        for (triplet in triplets) {
            NotificationAction.cancelNotification(context, triplet.reminderEvent.notificationId)

            Log.i(
                LogTags.REMINDER,
                String.format("Show reminder event reID %d for %s", triplet.reminderEvent.reminderEventId, triplet.reminderEvent.medicineName)
            )
        }

        // Schedule remaining repeats for all reminders
        val remainingRepeats = triplets[0].reminderEvent.remainingRepeats
        if (remainingRepeats != 0 && this.isRepeatReminders) {
            ReminderProcessor.requestRepeat(context, reminderIds, reminderEventIds, repeatTimeSeconds, remainingRepeats);
        }

        // Show notifications for all reminders
        showNotification(triplets, reminderDateTime)

    }

    private fun showNotification(triplets: List<NotificationTriplet>, reminderDateTime: LocalDateTime) {
        if (canShowNotifications()) {
            val notifications = Notifications(context)
            val notificationId =
                notifications.showNotification(
                    TimeHelper.minutesToTimeString(context, reminderDateTime.hour * 60L + reminderDateTime.minute),
                    triplets
                )

            for (triplet in triplets) {
                triplet.reminderEvent.notificationId = notificationId
                medicineRepository.updateReminderEvent(triplet.reminderEvent)
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
            if (lastReminderEvent != null && lastReminderEvent.status == ReminderEvent.ReminderStatus.TAKEN) {
                if (isWindowedInterval && TimeHelper.secondsSinceEpochToLocalDate(
                        lastReminderEvent.remindedTimestamp,
                        ZoneId.systemDefault()
                    ) !== TimeHelper.secondsSinceEpochToLocalDate(reminderEvent.remindedTimestamp, ZoneId.systemDefault())
                ) {
                    return 0
                }

                return (lastReminderEvent.processedTimestamp / 60).toInt()
            } else {
                return 0
            }
        }
    }
}
