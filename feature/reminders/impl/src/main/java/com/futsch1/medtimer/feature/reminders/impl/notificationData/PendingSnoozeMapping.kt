package com.futsch1.medtimer.feature.reminders.impl.notificationData

import com.futsch1.medtimer.core.domain.model.PendingSnooze
import com.futsch1.medtimer.feature.reminders.api.notificationData.ReminderNotificationData

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
