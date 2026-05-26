package com.futsch1.medtimer.feature.reminders.notificationData

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import java.time.Instant

class ReminderNotificationData(
    var remindInstant: Instant,
    var reminderIds: List<Int> = listOf(),
    var reminderEventIds: List<Int> = listOf(),
    var notificationId: Int = -1
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

    fun removeReminderEventIds(reminderEventIds: List<Int>): ReminderNotificationData {
        val newReminderEventIds = mutableListOf<Int>()
        val newReminderIds = mutableListOf<Int>()
        for (i in this.reminderEventIds.indices) {
            if (!reminderEventIds.contains(this.reminderEventIds[i])) {
                newReminderEventIds.add(this.reminderEventIds[i])
                newReminderIds.add(reminderIds[i])
            }
        }

        return ReminderNotificationData(
            remindInstant,
            newReminderIds,
            newReminderEventIds,
            notificationId
        )
    }

    override fun toString(): String {
        return "rIDs $reminderIds rEIDs $reminderEventIds nID $notificationId @ $remindInstant"
    }

    companion object {

        fun fromArrays(
            reminderIds: List<Int>,
            reminderEventIds: List<Int>,
            remindInstant: Instant,
            notificationId: Int = -1
        ): ReminderNotificationData {
            return ReminderNotificationData(remindInstant, reminderIds, reminderEventIds, notificationId)
        }

        fun fromScheduledReminders(reminders: List<ScheduledReminder>): ReminderNotificationData {
            val reminderIds = mutableListOf<Int>()
            val reminderEventIds = mutableListOf<Int>()
            val firstTimestamp = reminders.first().timestamp

            for (reminder in reminders) {
                // Reminders shall be raised together if they are due in the same minute
                if (reminder.timestamp.epochSecond / 60 == firstTimestamp.epochSecond / 60) {
                    reminderIds.add(reminder.reminder.id)
                    reminderEventIds.add(0)
                }
            }

            return ReminderNotificationData(
                firstTimestamp, reminderIds, reminderEventIds, -1
            )
        }

        fun fromReminderEvent(reminderEvent: ReminderEvent): ReminderNotificationData {
            val reminderIds = listOf(reminderEvent.reminderId)
            val reminderEventIds = listOf(reminderEvent.reminderEventId)
            return ReminderNotificationData(
                reminderEvent.remindedTimestamp, reminderIds, reminderEventIds
            )
        }
    }
}
