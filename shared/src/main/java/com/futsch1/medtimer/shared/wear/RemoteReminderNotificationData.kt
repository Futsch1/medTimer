package com.futsch1.medtimer.shared.wear

data class RemoteReminderNotificationData (
    val timestamp: String,
    val reminderId:Int,
    val medicineName:String,
    val reminderEventId:Int,
)