package com.futsch1.medtimer.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import androidx.core.text.bold
import androidx.core.text.color
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.R
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.preferences.PreferencesNames.HIDE_MED_NAME
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d+/\\d+)\\)")

    @JvmStatic
    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    @SuppressLint("DefaultLocale")
    fun getMedicineNameWithStockTextInternal(
        context: Context,
        medicine: Medicine,
        notification: Boolean
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder().bold {
            append(
                getMedicineName(
                    context,
                    medicine,
                    notification
                )
            )
        }
        if (medicine.isStockManagementActive) {
            builder.append(" (").append(getStockText(context, medicine))
                .append(getOutOfStockText(context, medicine)).append(")")
        }
        return builder
    }

    @JvmStatic
    fun getStockText(context: Context, medicine: Medicine): String {
        return context.getString(
            R.string.medicine_stock_string,
            formatAmount(medicine.amount, medicine.unit)
        )
    }

    @JvmStatic
    fun getOutOfStockText(
        context: Context,
        medicine: Medicine
    ): SpannableStringBuilder {
        if (medicine.isOutOfStock) {
            return SpannableStringBuilder().append(" ")
                .color(context.getColor(android.R.color.holo_red_dark)) { append("⚠") }
        }
        return SpannableStringBuilder()
    }

    @JvmStatic
    fun getMedicineNameWithStockText(context: Context, medicine: Medicine): SpannableStringBuilder {
        return getMedicineNameWithStockTextInternal(context, medicine, false)
    }

    @JvmStatic
    fun getMedicineName(
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

class AmountTextWatcher(val textEditInputEditText: TextInputEditText) : TextWatcher {
    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
        // Intentionally empty
    }

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        // Intentionally empty
    }

    override fun afterTextChanged(s: Editable?) {
        if (MedicineHelper.parseAmount(s.toString()) == null) {
            textEditInputEditText.error =
                textEditInputEditText.context.getString(R.string.invalid_amount)
        }
    }

}
