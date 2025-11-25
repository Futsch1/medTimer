package com.futsch1.medtimer.reminders.notificationData

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.reminders.NotificationReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor
import com.futsch1.medtimer.reminders.ReminderWork
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderNotificationData(
    var remindInstant: Instant,
    var reminderIds: IntArray = IntArray(0),
    var reminderEventIds: IntArray = IntArray(0),
    var notificationReminderEvents: List<NotificationReminderEvent> = emptyList(),
    val medicineRepository: MedicineRepository? = null,
    val notificationName: String = "RaisedNotification"
) {
    var valid: Boolean = reminderIds.isNotEmpty()
    var notificationId: Int = -1

    init {
        // The class can be either initialized via the reminder IDs or via the notification reminder events
        if (reminderIds.isEmpty() && notificationReminderEvents.isEmpty()) {
            valid = false
        }
        if (reminderIds.size != reminderEventIds.size) {
            valid = false
        }
        if (notificationReminderEvents.isEmpty()) {
            if (medicineRepository != null) {
                val result = mutableListOf<NotificationReminderEvent>()

                for (i in reminderIds.indices) {
                    val reminderEvent = medicineRepository.getReminderEvent(reminderEventIds[i])

                    if (reminderEvent != null) {
                        val reminder = medicineRepository.getReminder(reminderIds[i])
                        val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
                        result.add(NotificationReminderEvent(reminder, reminderEvent, medicine))
                    }
                }
                notificationReminderEvents = result
            }
        } else {
            reminderIds = notificationReminderEvents.map { it.reminder.reminderId }.toIntArray()
            reminderEventIds = notificationReminderEvents.map { it.reminderEvent.reminderEventId }.toIntArray()
        }
    }

    fun createReminderEvents(numberOfRepeats: Int) {
        val notificationReminderEvents = ArrayList<NotificationReminderEvent>()
        for (i in reminderIds.indices) {
            val reminderId = reminderIds[i]
            val reminderEventId = reminderEventIds[i]

            val reminder = medicineRepository?.getReminder(reminderId)
            if (reminder == null) {
                Log.e(LogTags.REMINDER, String.format("Could not find reminder rID %d in database", reminderId))
                valid = false
                return
            }

            val medicine = medicineRepository.getMedicine(reminder.medicineRelId)
            var reminderEvent = getEvent(medicineRepository, reminderEventId, reminder)
            if (reminderEvent == null) {
                reminderEvent = buildAndInsertReminderEvent(medicineRepository, medicine!!, reminder, numberOfRepeats)
            }
            notificationReminderEvents.add(NotificationReminderEvent(reminder, reminderEvent, medicine))
        }
        this.notificationReminderEvents = notificationReminderEvents
    }

    fun delayBy(delaySeconds: Int) {
        remindInstant = remindInstant.plusSeconds(delaySeconds.toLong())
    }

    fun getPendingIntent(context: Context): PendingIntent {
        val reminderIntent = ReminderProcessor.getReminderAction(context)
        toIntent(reminderIntent)
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventIds[0], reminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun toIntent(intent: Intent) {
        intent.putExtra(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds)
        intent.putExtra(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds)
        intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.toEpochMilli())
        intent.putExtra(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
    }

    fun toBuilder(builder: Data.Builder) {
        builder.putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds)
        builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds)
        builder.putLong(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.toEpochMilli())
        builder.putInt(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
    }

    override fun toString(): String {
        return notificationReminderEvents.toString()
    }

    fun getLocalDateTime(): LocalDateTime {
        return remindInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun getRemindTime(context: Context): String {
        val remindTime = getLocalDateTime()
        return TimeHelper.minutesToTimeString(context, remindTime.hour * 60L + remindTime.minute)
    }

    fun filterAutomaticallyTaken(): ReminderNotificationData {
        return ReminderNotificationData(
            remindInstant,
            notificationReminderEvents = notificationReminderEvents.stream().filter { !it.reminder.automaticallyTaken }.toList(),
            notificationName = notificationName
        )
    }

    private fun getEvent(medicineRepository: MedicineRepository, reminderEventId: Int, reminder: Reminder): ReminderEvent? {
        return if (reminderEventId != 0) {
            medicineRepository.getReminderEvent(reminderEventId)
        } else {
            // We might have created the reminder event already
            medicineRepository.getReminderEvent(reminder.reminderId, remindInstant.epochSecond)
        }
    }

    private fun buildAndInsertReminderEvent(
        medicineRepository: MedicineRepository, medicine: FullMedicine, reminder: Reminder, numberOfRepeats: Int
    ): ReminderEvent {
        val reminderEvent: ReminderEvent = ReminderWork.buildReminderEvent(remindInstant.epochSecond, medicine, reminder, medicineRepository)
        reminderEvent.remainingRepeats = numberOfRepeats
        reminderEvent.reminderEventId = medicineRepository.insertReminderEvent(reminderEvent).toInt()
        return reminderEvent
    }

    companion object {

        fun fromBundle(bundle: Bundle, medicineRepository: MedicineRepository?): ReminderNotificationData {
            val reminderIds = getReminderIds(bundle)
            val reminderEventIds = getReminderEventIds(bundle)
            val remindInstant = Instant.ofEpochMilli(bundle.getLong(ActivityCodes.EXTRA_REMIND_INSTANT))

            val raisedNotification = fromArrays(medicineRepository, reminderIds, reminderEventIds, remindInstant)
            raisedNotification.notificationId = bundle.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1)

            return raisedNotification
        }

        fun forwardToBuilder(bundle: Bundle, builder: Data.Builder) {
            builder.putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, getReminderIds(bundle))
            builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, getReminderEventIds(bundle))
            builder.putLong(ActivityCodes.EXTRA_REMIND_INSTANT, bundle.getLong(ActivityCodes.EXTRA_REMIND_INSTANT))
            builder.putInt(ActivityCodes.EXTRA_NOTIFICATION_ID, bundle.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1))
        }

        fun getReminderIds(bundle: Bundle): IntArray {
            return bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!
        }

        fun getReminderEventIds(bundle: Bundle): IntArray {
            return bundle.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!
        }

        fun fromArrays(
            medicineRepository: MedicineRepository?,
            reminderIds: IntArray,
            reminderEventIds: IntArray,
            remindInstant: Instant
        ): ReminderNotificationData {
            return ReminderNotificationData(remindInstant, reminderIds, reminderEventIds, medicineRepository = medicineRepository)
        }

        fun fromScheduledReminders(reminders: List<ScheduledReminder>): ReminderNotificationData {
            val reminderIds = mutableListOf<Int>()
            val reminderEventIds = mutableListOf<Int>()
            val medicineNames = mutableListOf<String>()
            val firstTimestamp = reminders.first().timestamp

            for (reminder in reminders) {
                if (reminder.timestamp == firstTimestamp) {
                    reminderIds.add(reminder.reminder().reminderId)
                    reminderEventIds.add(0)
                    medicineNames.add(reminder.medicine().medicine.name)
                }
            }

            return ReminderNotificationData(
                firstTimestamp, reminderIds.toIntArray(), reminderEventIds.toIntArray(), notificationName = medicineNames.joinToString(", ")
            )
        }

        fun fromReminderEvent(reminderEvent: ReminderEvent): ReminderNotificationData {
            val reminderIds = intArrayOf(reminderEvent.reminderId)
            val reminderEventIds = intArrayOf(reminderEvent.reminderEventId)
            val remindInstant = Instant.ofEpochSecond(reminderEvent.remindedTimestamp)
            return ReminderNotificationData(
                remindInstant, reminderIds, reminderEventIds, notificationName = "fromReminderEvent"
            )
        }

        fun fromInputData(inputData: Data, medicineRepository: MedicineRepository? = null): ReminderNotificationData {
            val reminderIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!
            val reminderEventIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!
            val remindInstant = Instant.ofEpochMilli(inputData.getLong(ActivityCodes.EXTRA_REMIND_INSTANT, 0))
            val notificationId = inputData.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1)
            val reminderNotificationData =
                ReminderNotificationData(
                    remindInstant,
                    reminderIds,
                    reminderEventIds,
                    notificationName = "fromInputData",
                    medicineRepository = medicineRepository
                )
            reminderNotificationData.notificationId = notificationId
            return reminderNotificationData
        }
    }
}