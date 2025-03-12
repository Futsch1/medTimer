package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.preferences.PreferencesNames.HIDE_MED_NAME
import java.text.NumberFormat
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d?/\\d?)\\)")

    @JvmStatic
    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    @SuppressLint("DefaultLocale")
    fun getMedicineNameWithStockTextInternal(
        context: Context,
        medicine: Medicine,
        notification: Boolean
    ): String {
        val name = getMedicineName(context, medicine, notification)
        return if (medicine.isStockManagementActive) {
            "$name (" + context.getString(
                R.string.medicine_stock_string,
                formatAmount(medicine.amount, medicine.unit),
                if (medicine.amount <= medicine.outOfStockReminderThreshold) " ⚠)" else ")"
            )
        } else {
            name
        }
    }

    @JvmStatic
    fun getMedicineNameWithStockText(context: Context, medicine: Medicine): String {
        return getMedicineNameWithStockTextInternal(context, medicine, false)
    }

    @JvmStatic
    fun getMedicineNameWithStockTextForNotification(context: Context, medicine: Medicine): String {
        return getMedicineNameWithStockTextInternal(context, medicine, true)
    }

    private fun getMedicineName(
        context: Context,
        medicine: Medicine,
        notification: Boolean
    ): String {
        return if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(HIDE_MED_NAME, false) && notification
        ) {
            medicine.name[0] + "*".repeat(medicine.name.length - 1)
        } else {
            medicine.name
        }
    }

    @JvmStatic
    fun formatAmount(amount: Double, unit: String): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.minimumFractionDigits = 0
        numberFormat.maximumFractionDigits = 2
        return numberFormat.format(amount) + if (unit.isEmpty()) "" else " $unit"
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
