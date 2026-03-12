package com.futsch1.medtimer.preferences

data class MedTimerSettings(
    val weekendTime: Int,
    val weekendMode: Boolean,
    val weekendDays: Set<String>
)