package com.futsch1.medtimer.reminders.notificationData

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderProcessor
import java.time.Instant

class ReminderNotificationData(
    var remindInstant: Instant,
    var reminderIds: IntArray = IntArray(0),
    var reminderEventIds: IntArray = IntArray(0),
    var notificationId: Int = -1,
    val notificationName: String = "Notification"
) {
    var valid: Boolean = reminderIds.isNotEmpty()

    init {
        // The class can be either initialized via the reminder IDs or via the notification reminder events
        if (reminderIds.isEmpty()) {
            valid = false
        }
        if (reminderIds.size != reminderEventIds.size) {
            valid = false
        }
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
        intent.putExtra(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.epochSecond)
        intent.putExtra(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
    }

    fun toBuilder(builder: Data.Builder) {
        builder.putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds)
        builder.putIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST, reminderEventIds)
        builder.putLong(ActivityCodes.EXTRA_REMIND_INSTANT, remindInstant.epochSecond)
        builder.putInt(ActivityCodes.EXTRA_NOTIFICATION_ID, notificationId)
    }

    override fun toString(): String {
        return notificationName
    }

    companion object {

        fun fromBundle(bundle: Bundle): ReminderNotificationData {
            val reminderIds = getReminderIds(bundle)
            val reminderEventIds = getReminderEventIds(bundle)
            val remindInstant = Instant.ofEpochSecond(bundle.getLong(ActivityCodes.EXTRA_REMIND_INSTANT))
            val notificationId = bundle.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1)

            return fromArrays(reminderIds, reminderEventIds, remindInstant, notificationId)
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
            reminderIds: IntArray,
            reminderEventIds: IntArray,
            remindInstant: Instant,
            notificationId: Int = -1
        ): ReminderNotificationData {
            return ReminderNotificationData(remindInstant, reminderIds, reminderEventIds, notificationId, notificationName = "fromArrays")
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
                firstTimestamp, reminderIds.toIntArray(), reminderEventIds.toIntArray(), -1, medicineNames.joinToString(", ")
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

        fun fromInputData(inputData: Data): ReminderNotificationData {
            val reminderIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!
            val reminderEventIds = inputData.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!
            val remindInstant = Instant.ofEpochSecond(inputData.getLong(ActivityCodes.EXTRA_REMIND_INSTANT, 0))
            val notificationId = inputData.getInt(ActivityCodes.EXTRA_NOTIFICATION_ID, -1)
            val reminderNotificationData =
                ReminderNotificationData(
                    remindInstant,
                    reminderIds,
                    reminderEventIds,
                    notificationId,
                    notificationName = "fromInputData"
                )
            return reminderNotificationData
        }
    }
}