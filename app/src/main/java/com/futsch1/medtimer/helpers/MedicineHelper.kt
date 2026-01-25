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
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Medicine
import com.futsch1.medtimer.preferences.PreferencesNames.HIDE_MED_NAME
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.text.ParseException
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
        fullMedicine: FullMedicine
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder().bold {
            append(
                getMedicineName(
                    context,
                    fullMedicine.medicine,
                    false
                )
            )
        }
        if (fullMedicine.isStockManagementActive) {
            builder.append(" (").append(getStockText(context, fullMedicine.medicine))
                .append(getOutOfStockText(context, fullMedicine)).append(")")
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
        fullMedicine: FullMedicine
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (fullMedicine.isOutOfStock) {
            builder.append(" ")
                .color(context.getColor(android.R.color.holo_red_dark)) { append("⚠") }
        }
        if (fullMedicine.medicine.hasExpired()) {
            builder.append(" ")
                .color(context.getColor(android.R.color.holo_red_dark)) { append("\uD83D\uDEAB") }
        }
        return builder
    }

    @JvmStatic
    fun getMedicineNameWithStockText(context: Context, fullMedicine: FullMedicine): SpannableStringBuilder {
        return getMedicineNameWithStockTextInternal(context, fullMedicine)
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

    fun parseAmount(amount: String?): Double? {
        val numberRegex = Pattern.compile("(?:\\d|\\.\\d)[.,\\s\\d]*")
        val matcher = numberRegex.matcher(amount ?: "")

        return if (matcher.find() && matcher.group(0) != null) {
            val numberFormat = NumberFormat.getNumberInstance()
            try {
                numberFormat.parse(matcher.group(0)!!.replace(" ", ""))?.toDouble()
            } catch (_: ParseException) {
                null
            }
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
