package com.futsch1.medtimer.medicine.editMedicine

import com.futsch1.medtimer.ReminderNotificationChannelManager
import com.futsch1.medtimer.database.Medicine

fun importanceValueToIndex(medicine: Medicine): Int {
    if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.DEFAULT.value) {
        return 0
    }
    if (medicine.notificationImportance == ReminderNotificationChannelManager.Importance.HIGH.value) {
        return if (medicine.showNotificationAsAlarm) 2 else 1
    }
    return 0
}

fun importanceIndexToMedicine(index: Int, medicine: Medicine) {
    when (index) {
        0 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT.value
            medicine.showNotificationAsAlarm = false
        }

        1 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.HIGH.value
            medicine.showNotificationAsAlarm = false
        }

        2 -> {
            medicine.notificationImportance = ReminderNotificationChannelManager.Importance.HIGH.value
            medicine.showNotificationAsAlarm = true
        }
    }
}
