package com.futsch1.medtimer.reminders

import android.app.PendingIntent
import android.content.Context
import androidx.work.Data
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.ScheduledReminder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduledNotification(val reminderIds: List<Int>, val reminderEventIds: List<Int>, var remindInstant: Instant, val notificationName: String) {
    // Scheduled notification represents a notification in flight - either when it is just scheduled via the alarm manager or snoozed.
    // An object of this class is not passed via intents, but this class is used to manage the data and create the corresponding actions.

    fun isEmpty(): Boolean {
        return reminderIds.isEmpty()
    }

    fun getLocalDateTime(): LocalDateTime {
        return remindInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun getPendingIntent(context: Context): PendingIntent {
        val reminderIntent = ReminderProcessor.getReminderAction(context, reminderIds.toIntArray(), reminderEventIds.toIntArray(), getLocalDateTime())
        // Use the reminderEventId as request code to ensure unique PendingIntent for each reminder event
        return PendingIntent.getBroadcast(context, reminderEventIds[0], reminderIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun getReminderWorkData(): Data {
        return Data.Builder()
            .putIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST, reminderIds.toIntArray())
            .putLong(ActivityCodes.EXTRA_REMINDER_DATE, getLocalDateTime().toLocalDate().toEpochDay())
            .putInt(ActivityCodes.EXTRA_REMINDER_TIME, getLocalDateTime().toLocalTime().toSecondOfDay())
            .build()
    }

    fun delayBy(delaySeconds: Int) {
        remindInstant = remindInstant.plusSeconds(delaySeconds.toLong())
    }

    companion object {
        fun fromScheduledReminders(reminders: List<ScheduledReminder>): ScheduledNotification {
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

            return ScheduledNotification(
                reminderIds, reminderEventIds, firstTimestamp,
                medicineNames.joinToString(", ")
            )
        }

        fun fromInputData(data: Data): ScheduledNotification {
            val reminderIds = data.getIntArray(ActivityCodes.EXTRA_REMINDER_ID_LIST)!!.toList()
            val reminderEventIds = data.getIntArray(ActivityCodes.EXTRA_REMINDER_EVENT_ID_LIST)!!.toList()
            val reminderDate = data.getLong(ActivityCodes.EXTRA_REMINDER_DATE, 0)
            val reminderTime = data.getInt(ActivityCodes.EXTRA_REMINDER_TIME, 0)
            val datePart = LocalDate.ofEpochDay(reminderDate)
            val timePart = LocalTime.ofSecondOfDay(reminderTime.toLong())
            return ScheduledNotification(
                reminderIds, reminderEventIds,
                LocalDateTime.of(datePart, timePart).atZone(ZoneId.systemDefault()).toInstant(),
                "fromInputData"
            )
        }
    }
}