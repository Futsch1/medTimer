package com.futsch1.medtimer.shared.wear


data class TakenData(
    val name: String,
    val reminderEventId:Int,
    val notificationId:Int,
    val active: Boolean,
)