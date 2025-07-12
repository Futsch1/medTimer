package com.futsch1.medtimer.wear

import com.futsch1.medtimer.reminders.RescheduleWork
import com.futsch1.medtimer.shared.wear.RemoteReminderNotificationData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val formatterWithoutOffset = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun createRemoteReminderNotificationData(reminderNotificationData: RescheduleWork.ReminderNotificationData?): RemoteReminderNotificationData? {
    if (reminderNotificationData == null ) {
        return null;
    }
    var timestamp = ""
    val ldt = LocalDateTime.ofInstant(reminderNotificationData.timestamp, ZoneId.systemDefault());
    timestamp = ldt.format(formatterWithoutOffset)
    return RemoteReminderNotificationData(
        timestamp,
        reminderNotificationData.reminderId,
        reminderNotificationData.medicineName,
        reminderNotificationData.reminderEventId
    )
}