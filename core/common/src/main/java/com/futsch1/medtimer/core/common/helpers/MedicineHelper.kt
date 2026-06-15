package com.futsch1.medtimer.core.common.helpers

import android.text.SpannableStringBuilder
import androidx.core.text.bold
import androidx.core.text.color
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.UserPreferences
import java.text.NumberFormat
import java.text.ParseException
import java.util.regex.Pattern

object MedicineHelper {
    private val CYCLIC_COUNT: Pattern = Pattern.compile(" (\\(\\d+/\\d+)\\)")
    private val AMOUNT_PATTERN: Pattern = Pattern.compile("(?:\\d|\\.\\d)[.,\\s\\d]*")
    // NumberFormat is not thread-safe; ThreadLocal gives each thread its own instance at zero per-call cost
    private val numberFormat: ThreadLocal<NumberFormat> = ThreadLocal.withInitial { NumberFormat.getNumberInstance() }

    fun normalizeMedicineName(medicineName: String): String {
        return CYCLIC_COUNT.matcher(medicineName).replaceAll("")
    }

    fun getMedicineName(
        medicine: Medicine,
        notification: Boolean,
        userPreferences: UserPreferences
    ): String {
        return if (userPreferences.hideMedicineName && notification) {
            medicine.name[0] + "*".repeat(medicine.name.length - 1)
        } else {
            medicine.name
        }
    }

    fun getStockIcons(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (medicine.isOutOfStock()) {
            builder.color(0xffcc0000.toInt()) { bold { append("⚠") } }
        }
        val expiredIcon = getExpiredIcon(medicine)
        if (expiredIcon.isNotEmpty()) {
            if (builder.isNotEmpty()) {
                builder.append(" ")
            }
            builder.append(expiredIcon)
        }
        return builder
    }

    fun getExpiredIcon(medicine: Medicine): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (medicine.hasExpired()) {
            builder.color(0xffcc0000.toInt()) { bold { append("\uD83D\uDEAB") } }
        }
        return builder
    }

    fun formatAmount(amount: Double, unit: String): String {
        val fmt = numberFormat.get() ?: return ""
        fmt.minimumFractionDigits = 0
        fmt.maximumFractionDigits = 2
        return fmt.format(amount) + if (unit.isEmpty()) "" else " $unit"
    }

    fun parseAmount(amount: String?): Double? {
        val matcher = AMOUNT_PATTERN.matcher(amount ?: "")

        return if (matcher.find() && matcher.group(0) != null) {
            try {
                numberFormat.get()?.parse(matcher.group(0)!!.replace(" ", ""))?.toDouble()
            } catch (_: ParseException) {
                null
            }
        } else {
            null
        }
    }
}
