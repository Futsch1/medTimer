package com.futsch1.medtimer.reminders.notificationData

import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.helpers.TimeHelper
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ReminderEventType
import java.time.LocalTime
import java.time.ZoneId

class ReminderNotification(val reminderNotificationParts: List<ReminderNotificationPart>, val reminderNotificationData: ReminderNotificationData) {
    fun filterAutomaticallyTaken(): ReminderNotification {
        return filter { !it.reminder.automaticallyTaken }
    }

    fun filterAlreadyProcessed(): ReminderNotification {
        return filter { it.reminderEvent.status == ReminderEvent.ReminderStatus.RAISED }
    }

    fun isOutOfStockNotification(): Boolean {
        return reminderNotificationParts.size == 1 && reminderNotificationParts[0].reminderEvent.reminderType == ReminderEventType.OUT_OF_STOCK
    }

    fun isExpirationDateNotification(): Boolean {
        return reminderNotificationParts.size == 1 && reminderNotificationParts[0].reminderEvent.reminderType == ReminderEventType.EXPIRATION_DATE
    }

    private fun filter(predicate: (ReminderNotificationPart) -> Boolean): ReminderNotification {
        val reminderNotificationParts = mutableListOf<ReminderNotificationPart>()
        val removedReminderEventIds = mutableListOf<Int>()
        for (part in this.reminderNotificationParts) {
            if (predicate(part)) {
                reminderNotificationParts.add(part)
            } else {
                removedReminderEventIds.add(part.reminderEvent.reminderEventId)
            }
        }
        return ReminderNotification(
            reminderNotificationParts,
            reminderNotificationData.removeReminderEventIds(removedReminderEventIds)
        )
    }

    private fun getRemindedTime(): LocalTime {
        return TimeHelper.secondsSinceEpochToLocalTime(reminderNotificationData.remindInstant.epochSecond, ZoneId.systemDefault())
    }

    fun getRemindTime(timeFormatter: TimeFormatter): String {
        val remindTime = getRemindedTime()
        return timeFormatter.minutesToTimeString(remindTime.hour * 60 + remindTime.minute)
    }

    override fun toString(): String {
        return reminderNotificationData.toString()
    }
}
