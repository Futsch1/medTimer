package com.futsch1.medtimer.feature.reminders.notificationData

import com.futsch1.medtimer.core.domain.model.PendingSnooze

fun ReminderNotificationData.toPendingSnooze() = PendingSnooze(
    remindInstant = remindInstant,
    reminderIds = reminderIds,
    reminderEventIds = reminderEventIds,
    notificationId = notificationId
)

fun PendingSnooze.toReminderNotificationData() = ReminderNotificationData(
    remindInstant = remindInstant,
    reminderIds = reminderIds,
    reminderEventIds = reminderEventIds,
    notificationId = notificationId
)
