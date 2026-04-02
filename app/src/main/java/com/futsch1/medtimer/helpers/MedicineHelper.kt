package com.futsch1.medtimer.helpers

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import androidx.core.text.color
import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.MedicineEntity
import com.futsch1.medtimer.model.UserPreferences
import java.text.NumberFormat
import java.text.ParseException
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d+/\\d+)\\)")

    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    fun getMedicineName(
        medicine: MedicineEntity,
        notification: Boolean,
        userPreferences: UserPreferences
    ): String {
        return if (userPreferences.hideMedicineName && notification) {
            medicine.name[0] + "*".repeat(medicine.name.length - 1)
        } else {
            medicine.name
        }
    }

    fun getStockIcons(fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (fullMedicine.isOutOfStock) {
            builder.color(0xffcc0000.toInt()) { bold { append("⚠") } }
        }
        val expiredIcon = getExpiredIcon(fullMedicine)
        if (expiredIcon.isNotEmpty()) {
            if (builder.isNotEmpty()) {
                builder.append(" ")
            }
            builder.append(expiredIcon)
        }
        return builder
    }

    fun getExpiredIcon(fullMedicine: FullMedicineEntity): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (fullMedicine.medicine.hasExpired()) {
            builder.color(0xffcc0000.toInt()) { bold { append("\uD83D\uDEAB") } }
        }
        return builder
    }

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
