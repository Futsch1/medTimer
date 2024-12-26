package com.futsch1.medtimer.medicine.editMedicine

import android.content.res.Resources
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine

fun stockReminderValueToString(
    value: Medicine.MedicationStockReminder,
    resources: Resources
): String {
    val stockReminderTexts: Array<String> =
        resources.getStringArray(R.array.stock_reminder)

    return when (value) {
        Medicine.MedicationStockReminder.OFF -> stockReminderTexts[0]
        Medicine.MedicationStockReminder.ONCE -> stockReminderTexts[1]
        Medicine.MedicationStockReminder.ALWAYS -> stockReminderTexts[2]
    }
}

fun stockReminderStringToValue(
    stockReminder: String,
    resources: Resources
): Medicine.MedicationStockReminder {
    val stockReminderTexts: Array<String> =
        resources.getStringArray(R.array.stock_reminder)
    return when (stockReminder) {
        stockReminderTexts[0] -> Medicine.MedicationStockReminder.OFF
        stockReminderTexts[1] -> Medicine.MedicationStockReminder.ONCE
        stockReminderTexts[2] -> Medicine.MedicationStockReminder.ALWAYS
        else -> Medicine.MedicationStockReminder.OFF
    }
}