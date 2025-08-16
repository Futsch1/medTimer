package com.futsch1.medtimer.shared.wear


data class WearData(
    val name: String,
    val reminderNotificationData: RemoteReminderNotificationData? = null,
)