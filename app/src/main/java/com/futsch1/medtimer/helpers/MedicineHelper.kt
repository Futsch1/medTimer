package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import java.text.NumberFormat
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d?/\\d?)\\)")

    @JvmStatic
    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    @JvmStatic
    @SuppressLint("DefaultLocale")
    fun getMedicineNameWithStockText(context: Context, medicine: Medicine): String {
        return if (medicine.isStockManagementActive) {
            medicine.name + " (" + context.getString(
                R.string.medicine_stock_string,
                formatAmount(medicine.amount),
                if (medicine.amount <= medicine.outOfStockReminderThreshold) " ⚠)" else ")"
            )
        } else {
            medicine.name
        }
    }

    @JvmStatic
    fun formatAmount(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 2
        return numberFormat.format(amount)
    }

    fun parseAmount(amount: String): Double? {
        val numberRegex = Pattern.compile("\\d+(?:[.,]\\d+)?")
        val matcher = numberRegex.matcher(amount)

        return if (matcher.find() && matcher.group(0) != null) {
            matcher.group(0)?.replace(',', '.')?.toDoubleOrNull()
        } else {
            null
        }
    }
}
