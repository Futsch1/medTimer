package com.futsch1.medtimer.medicine.editMedicine

import android.content.res.Resources
import com.futsch1.medtimer.R
import com.futsch1.medtimer.ReminderNotificationChannelManager

fun importanceValueToString(value: Int, resources: Resources): String {
    val importanceTexts: Array<String> =
        resources.getStringArray(R.array.notification_importance)

    if (value == ReminderNotificationChannelManager.Importance.DEFAULT.value) {
        return importanceTexts[0]
    }
    if (value == ReminderNotificationChannelManager.Importance.HIGH.value) {
        return importanceTexts[1]
    }
    return importanceTexts[0]
}

fun importanceStringToValue(importance: String, resources: Resources): Int {
    var value = ReminderNotificationChannelManager.Importance.DEFAULT.value
    val importanceTexts: Array<String> =
        resources.getStringArray(R.array.notification_importance)
    if (importance == importanceTexts[1]) {
        value = ReminderNotificationChannelManager.Importance.HIGH.value
    }
    return value
}
