package com.futsch1.medtimer.medicine.editMedicine

import android.content.res.Resources
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine

fun stockReminderValueToString(
    value: Medicine.OutOfStockReminderType,
    resources: Resources
): String {
    val stockReminderTexts: Array<String> =
        resources.getStringArray(R.array.stock_reminder)

    return when (value) {
        Medicine.OutOfStockReminderType.OFF -> stockReminderTexts[0]
        Medicine.OutOfStockReminderType.ONCE -> stockReminderTexts[1]
        Medicine.OutOfStockReminderType.ALWAYS -> stockReminderTexts[2]
    }
}

fun stockReminderStringToValue(
    stockReminder: String,
    resources: Resources
): Medicine.OutOfStockReminderType {
    val stockReminderTexts: Array<String> =
        resources.getStringArray(R.array.stock_reminder)
    return when (stockReminder) {
        stockReminderTexts[0] -> Medicine.OutOfStockReminderType.OFF
        stockReminderTexts[1] -> Medicine.OutOfStockReminderType.ONCE
        stockReminderTexts[2] -> Medicine.OutOfStockReminderType.ALWAYS
        else -> Medicine.OutOfStockReminderType.OFF
    }
}
