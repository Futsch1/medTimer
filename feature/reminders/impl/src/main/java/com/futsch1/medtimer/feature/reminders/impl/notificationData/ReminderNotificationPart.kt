package com.futsch1.medtimer.feature.reminders.impl.notificationData

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent

data class ReminderNotificationPart(
    val reminder: Reminder,
    val reminderEvent: ReminderEvent,
    val medicine: Medicine
)

fun ReminderNotificationPart.effectiveImportance(): Medicine.NotificationImportance =
    when (reminder.notificationImportance) {
        Reminder.NotificationImportance.SAME_AS_MEDICINE -> medicine.notificationImportance
        Reminder.NotificationImportance.DEFAULT -> Medicine.NotificationImportance.DEFAULT
        Reminder.NotificationImportance.HIGH, Reminder.NotificationImportance.HIGH_AND_ALARM -> Medicine.NotificationImportance.HIGH
    }

fun ReminderNotificationPart.effectiveShowAsAlarm(): Boolean =
    when (reminder.notificationImportance) {
        Reminder.NotificationImportance.SAME_AS_MEDICINE -> medicine.showNotificationAsAlarm
        Reminder.NotificationImportance.HIGH_AND_ALARM -> true
        else -> false
    }

fun List<ReminderNotificationPart>.effectiveHighestImportance(): Medicine.NotificationImportance {
    for (part in this) {
        if (part.effectiveImportance() == Medicine.NotificationImportance.HIGH)
            return Medicine.NotificationImportance.HIGH
    }
    return Medicine.NotificationImportance.DEFAULT
}

fun List<ReminderNotificationPart>.channelForHighestImportance(): Medicine.ReminderChannel {
    for (part in this) {
        if (part.effectiveImportance() == Medicine.NotificationImportance.HIGH)
            return Medicine.ReminderChannel.HIGH
    }
    return Medicine.ReminderChannel.DEFAULT
}
