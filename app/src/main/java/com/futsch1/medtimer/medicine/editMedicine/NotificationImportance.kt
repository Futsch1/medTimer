package com.futsch1.medtimer.medicine.editMedicine

import com.futsch1.medtimer.ReminderNotificationChannelManager

fun importanceValueToIndex(value: Int): Int {
    if (value == ReminderNotificationChannelManager.Importance.DEFAULT.value) {
        return 0
    }
    if (value == ReminderNotificationChannelManager.Importance.HIGH.value) {
        return 1
    }
    return 0
}

fun importanceIndexToValue(index: Int): Int {
    return if (index == 0) ReminderNotificationChannelManager.Importance.DEFAULT.value else ReminderNotificationChannelManager.Importance.HIGH.value
}
